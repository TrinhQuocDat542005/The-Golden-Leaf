package com.example.datban.dto;

import com.example.datban.model.ThucDon;
import java.math.BigDecimal;

public record MenuItemResponse(
        Long idThucDon,
        String tenMon,
        BigDecimal gia,
        String moTa,
        String anh,
        String nhom
) {
    public static MenuItemResponse from(ThucDon item) {
        return new MenuItemResponse(
                item.getIdThucDon(), item.getTenMon(), item.getGia(), item.getMoTa() == null ? "" : item.getMoTa(), item.getAnh() == null ? "" : item.getAnh(),
                item.getNhom() == null ? "" : item.getNhom().name());
    }
}
