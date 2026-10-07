package com.example.datban.dto;

import com.example.datban.model.GioHang;
import java.math.BigDecimal;

public record BookingItemResponse(
        Long id,
        Long idDat,
        Long idThucDon,
        String tenMon,
        Integer soLuong,
        BigDecimal giaMon,
        BigDecimal thanhTien
) {
    public static BookingItemResponse from(GioHang item) {
        return new BookingItemResponse(item.getId(), item.getIdDat(), item.getIdThucDon(), item.getTenMon(),
                item.getSoLuong(), item.getGiaMon(), item.getThanhTien());
    }
}
