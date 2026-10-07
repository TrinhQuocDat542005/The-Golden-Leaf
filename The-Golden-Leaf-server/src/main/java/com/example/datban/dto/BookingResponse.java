package com.example.datban.dto;

import com.example.datban.model.DatBan;
import java.time.LocalDate;
import java.time.Instant;
import com.example.datban.model.BookingStatus;

public record BookingResponse(
        Long idDat,
        String email,
        String ten,
        LocalDate ngay,
        String khungGio,
        Integer soLuong,
        String ghiChu,
        String viTriBan,
        BookingStatus status,
        Instant holdExpiresAt,
        int reservedTables
) {
    public static BookingResponse from(DatBan booking) {
        return new BookingResponse(
                booking.getIdDat(), booking.getEmail(), booking.getTen(), booking.getNgay(),
                booking.getKhungGio(), booking.getSoLuong(), booking.getGhiChu(), booking.getViTriBan(),
                booking.getStatus(), booking.getHoldExpiresAt(), booking.getReservedTables());
    }
}
