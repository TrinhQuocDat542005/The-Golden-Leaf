package com.example.datban.dto;

import com.example.datban.model.DatBan;
import java.time.LocalDate;

public record BookingResponse(
        Long idDat,
        String email,
        String ten,
        LocalDate ngay,
        String khungGio,
        Integer soLuong,
        String ghiChu,
        String viTriBan
) {
    public static BookingResponse from(DatBan booking) {
        return new BookingResponse(
                booking.getIdDat(), booking.getEmail(), booking.getTen(), booking.getNgay(),
                booking.getKhungGio(), booking.getSoLuong(), booking.getGhiChu(), booking.getViTriBan());
    }
}
