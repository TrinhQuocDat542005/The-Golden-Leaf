package com.example.datban.service;

import com.example.datban.model.BanSlot;
import com.example.datban.repository.BanSlotRepository;
import com.example.datban.exception.BusinessRuleException;
import com.example.datban.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class BanSlotService {

    private final BanSlotRepository repo;
    private final java.time.Clock clock;
    private final java.time.ZoneId zone;

    public BanSlotService(BanSlotRepository repo, java.time.Clock clock,
            @org.springframework.beans.factory.annotation.Value("${app.booking.zone:Asia/Ho_Chi_Minh}") String zone) {
        this.repo = repo;
        this.clock = clock;
        this.zone = java.time.ZoneId.of(zone);
    }

    // Lấy tất cả slot
    public List<BanSlot> getAllSlots() {
        LocalDate today = LocalDate.now(clock.withZone(zone));
        return repo.findByNgayBetweenOrderByNgayAscKhungGioAsc(today, today.plusDays(6));
    }

    // Lấy slot theo ngày + khung
    public BanSlot getSlot(LocalDate ngay, String khungGio) {
        return repo.findByNgayAndKhungGio(ngay, khungGio)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khung giờ đã chọn"));
    }

    // Đặt bàn theo số lượng khách
    @Transactional
    public BanSlot datBan(LocalDate ngay, String khungGio, int soLuongKhach) {
        throw new BusinessRuleException("BOOKING_REQUIRED", "Giữ chỗ qua /api/datban/save để gắn sức chứa với đơn");
    }

    // Trả bàn
    @Transactional
    public BanSlot traBan(LocalDate ngay, String khungGio, int soLuongKhach) {
        throw new BusinessRuleException("BOOKING_REQUIRED", "Trả sức chứa qua /api/datban/{id}/cancel");
    }
}
