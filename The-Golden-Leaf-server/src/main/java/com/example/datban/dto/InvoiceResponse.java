package com.example.datban.dto;

import com.example.datban.model.HoaDon;
import java.math.BigDecimal;
import java.time.Instant;

public record InvoiceResponse(
        Long id,
        Long idDat,
        BigDecimal tienBan,
        BigDecimal tienAn,
        BigDecimal tongTien,
        String currency,
        Instant ngayGioThanhToan
) {
    public static InvoiceResponse from(HoaDon invoice) {
        return new InvoiceResponse(invoice.getId(), invoice.getIdDat(), invoice.getTienBan(), invoice.getTienAn(),
                invoice.getTongTien(), invoice.getCurrency(), invoice.getNgayGioThanhToan());
    }
}
