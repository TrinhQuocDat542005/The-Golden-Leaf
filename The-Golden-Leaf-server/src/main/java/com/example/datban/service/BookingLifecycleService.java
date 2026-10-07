package com.example.datban.service;

import com.example.datban.exception.BusinessRuleException;
import com.example.datban.exception.ResourceNotFoundException;
import com.example.datban.model.*;
import com.example.datban.repository.*;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

/** Every write locks the slot before the booking to avoid inconsistent capacity and lock ordering. */
@Service
public class BookingLifecycleService {
    private final DatBanRepository bookings;
    private final BanSlotRepository slots;
    private final Clock clock;
    private final Duration holdDuration;
    private final ZoneId restaurantZone;

    public BookingLifecycleService(DatBanRepository bookings, BanSlotRepository slots, Clock clock,
            @Value("${app.booking.hold-duration:PT15M}") Duration holdDuration,
            @Value("${app.booking.zone:Asia/Ho_Chi_Minh}") String zone) {
        if (holdDuration.isNegative() || holdDuration.isZero()) {
            throw new IllegalArgumentException("Booking hold duration must be positive");
        }
        this.bookings = bookings;
        this.slots = slots;
        this.clock = clock;
        this.holdDuration = holdDuration;
        this.restaurantZone = ZoneId.of(zone);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DatBan create(DatBan request, String key, String actorEmail) {
        if (key == null || !key.matches("[A-Za-z0-9_-]{1,128}")) {
            throw rule("INVALID_IDEMPOTENCY_KEY", "Cần Idempotency-Key từ 1 đến 128 ký tự chữ, số, _ hoặc -");
        }
        // Match MySQL's case-insensitive key uniqueness consistently on all database engines.
        key = key.toLowerCase(java.util.Locale.ROOT);
        checkOwner(request, actorEmail);
        BanSlot slot = slots.lockSlot(request.getNgay(), request.getKhungGio())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khung giờ đã chọn"));
        var replay = bookings.findByIdempotencyKey(key);
        if (replay.isPresent()) {
            DatBan existing = replay.get();
            if (!sameRequest(existing, request)) {
                throw rule("IDEMPOTENCY_CONFLICT", "Idempotency-Key đã được dùng với nội dung đặt bàn khác");
            }
            checkOwner(existing, actorEmail);
            return existing;
        }
        LocalDate today = LocalDate.now(clock.withZone(restaurantZone));
        if (request.getNgay().isBefore(today) || request.getNgay().isAfter(today.plusDays(6))) {
            throw rule("INVALID_BOOKING_DATE", "Chỉ nhận đặt bàn trong 7 ngày tính từ hôm nay");
        }
        java.time.LocalTime start = java.time.LocalTime.parse(slot.getKhungGio().split("-")[0]);
        if (!request.getNgay().atTime(start).atZone(restaurantZone).toInstant().isAfter(clock.instant())) {
            throw rule("INVALID_BOOKING_TIME", "Khung giờ đã bắt đầu, vui lòng chọn khung giờ khác");
        }
        if (request.getSoLuong() == null || request.getSoLuong() < 1 || request.getSoLuong() > 80) {
            throw rule("INVALID_GUEST_COUNT", "Số lượng khách phải từ 1 đến 80");
        }
        // Reclaim expired holds while holding the inventory lock, even before the scheduled sweep runs.
        for (DatBan expired : bookings.lockExpiredInSlot(request.getNgay(), request.getKhungGio(),
                BookingStatus.HOLDING, clock.instant())) {
            release(expired, slot, BookingStatus.EXPIRED);
        }
        int required = (request.getSoLuong() + 7) / 8;
        if (slot.getSoBanConLai() < required) {
            throw rule("NOT_ENOUGH_TABLES", "Không đủ bàn trống cho số lượng khách đã chọn");
        }
        slot.setSoBanConLai(slot.getSoBanConLai() - required);
        request.setIdempotencyKey(key);
        request.setReservedTables(required);
        request.setStatus(BookingStatus.HOLDING);
        request.setHoldExpiresAt(clock.instant().plus(holdDuration));
        return bookings.saveAndFlush(request);
    }

    /** Must be invoked inside a transaction. Scalar lookup avoids caching an unlocked booking entity. */
    public DatBan lock(Long id, String actorEmail) {
        var identity = bookings.slotIdentity(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt đặt bàn"));
        slots.lockSlot(identity.getNgay(), identity.getKhungGio())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khung giờ của lượt đặt bàn"));
        DatBan booking = bookings.lockBooking(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt đặt bàn"));
        checkOwner(booking, actorEmail);
        return booking;
    }

    public void requireHolding(DatBan booking) {
        if (booking.getStatus() != BookingStatus.HOLDING) {
            throw rule("INVALID_BOOKING_STATE", "Đơn không còn ở trạng thái giữ chỗ");
        }
        if (booking.getHoldExpiresAt() == null || !booking.getHoldExpiresAt().isAfter(clock.instant())) {
            throw rule("BOOKING_HOLD_EXPIRED", "Thời gian giữ chỗ đã hết, vui lòng đặt lại");
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DatBan confirm(Long id, String actorEmail) {
        DatBan booking = lock(id, actorEmail);
        if (booking.getStatus() == BookingStatus.CONFIRMED) { return booking; }
        requireHolding(booking);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setHoldExpiresAt(null);
        return booking;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DatBan cancel(Long id, String actorEmail) {
        DatBan booking = lock(id, actorEmail);
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.EXPIRED) {
            return booking;
        }
        if (booking.getStatus() != BookingStatus.HOLDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw rule("INVALID_BOOKING_STATE", "Không thể hủy đơn ở trạng thái hiện tại");
        }
        BanSlot slot = slots.findByNgayAndKhungGio(booking.getNgay(), booking.getKhungGio()).orElseThrow();
        release(booking, slot, isExpired(booking) ? BookingStatus.EXPIRED : BookingStatus.CANCELLED);
        return booking;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void expire(Long id) {
        DatBan booking = lock(id, null);
        if (isExpired(booking)) {
            BanSlot slot = slots.findByNgayAndKhungGio(booking.getNgay(), booking.getKhungGio()).orElseThrow();
            release(booking, slot, BookingStatus.EXPIRED);
        }
    }

    private boolean isExpired(DatBan booking) {
        return booking.getStatus() == BookingStatus.HOLDING && booking.getHoldExpiresAt() != null
                && !booking.getHoldExpiresAt().isAfter(clock.instant());
    }

    private void release(DatBan booking, BanSlot slot, BookingStatus next) {
        int remaining = slot.getSoBanConLai() + booking.getReservedTables();
        if (remaining > slot.getSoBanBanDau()) {
            throw rule("CAPACITY_CONFLICT", "Sức chứa khung giờ không nhất quán, cần kiểm tra dữ liệu");
        }
        slot.setSoBanConLai(remaining);
        booking.setStatus(next);
        booking.setHoldExpiresAt(null);
    }

    public static void checkOwner(DatBan booking, String actorEmail) {
        if (actorEmail != null && !actorEmail.equalsIgnoreCase(booking.getEmail())) {
            throw new org.springframework.security.access.AccessDeniedException("Không có quyền truy cập đơn đặt bàn");
        }
    }

    private boolean sameRequest(DatBan a, DatBan b) {
        return Objects.equals(a.getEmail(), b.getEmail()) && Objects.equals(a.getTen(), b.getTen())
                && Objects.equals(a.getNgay(), b.getNgay()) && Objects.equals(a.getKhungGio(), b.getKhungGio())
                && Objects.equals(a.getSoLuong(), b.getSoLuong()) && Objects.equals(a.getGhiChu(), b.getGhiChu())
                && Objects.equals(a.getViTriBan(), b.getViTriBan());
    }

    private BusinessRuleException rule(String code, String message) {
        return new BusinessRuleException(code, message);
    }
}
