package com.example.datban.dto;
import java.math.BigDecimal;

public record PaymentQuote(Long idDat,BigDecimal tienBan,BigDecimal tienAn,BigDecimal tongTien,String currency) {}
