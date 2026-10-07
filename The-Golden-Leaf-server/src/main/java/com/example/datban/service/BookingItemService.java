package com.example.datban.service;

import com.example.datban.dto.BookingItemRequest;
import com.example.datban.exception.BusinessRuleException;
import com.example.datban.exception.ResourceNotFoundException;
import com.example.datban.model.*;
import com.example.datban.repository.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

@Service
public class BookingItemService {
    private final BookingLifecycleService lifecycle;
    private final GioHangRepository items;
    private final ThucDonRepository menu;

    public BookingItemService(BookingLifecycleService lifecycle, GioHangRepository items, ThucDonRepository menu) {
        this.lifecycle = lifecycle;
        this.items = items;
        this.menu = menu;
    }

    /** The request is the entire cart, so retries replace rather than append duplicate lines. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<GioHang> replace(Long bookingId, List<BookingItemRequest> requests, String actorEmail) {
        if (requests == null || requests.size() > 100) {
            throw new BusinessRuleException("INVALID_CART", "Giỏ hàng không được vượt quá 100 món");
        }
        Set<Long> ids = new HashSet<>();
        for (var request : requests) {
            if (!Objects.equals(bookingId, request.idDat()) || !ids.add(request.idThucDon())
                    || request.soLuong() == null || request.soLuong() < 1 || request.soLuong() > 100) {
                throw new BusinessRuleException("INVALID_CART", "Giỏ hàng phải thuộc một đơn, không trùng món, số lượng 1–100");
            }
        }
        DatBan booking = lifecycle.lock(bookingId, actorEmail);
        var existing = items.findByIdDat(bookingId);
        boolean sameCart = existing.size() == requests.size() && requests.stream().allMatch(request ->
                existing.stream().anyMatch(item -> item.getIdThucDon().equals(request.idThucDon())
                        && item.getSoLuong().equals(request.soLuong())));
        if (sameCart && booking.getStatus() == BookingStatus.CONFIRMED) { return existing; }
        lifecycle.requireHolding(booking);
        if (sameCart) { return existing; }

        // Resolve every menu item before replacing any persisted line; a missing item rolls back the whole cart.
        var replacement = requests.stream().map(request -> {
            ThucDon dish = menu.findById(request.idThucDon())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy món ăn"));
            if (!dish.isActive()) {
                throw new BusinessRuleException("MENU_ITEM_UNAVAILABLE", "Món ăn đã ngừng phục vụ");
            }
            return new GioHang(bookingId, booking.getEmail(), dish.getIdThucDon(), dish.getTenMon(),
                    request.soLuong(), dish.getGia());
        }).toList();
        items.deleteAll(existing);
        items.flush();
        return items.saveAllAndFlush(replacement);
    }
}
