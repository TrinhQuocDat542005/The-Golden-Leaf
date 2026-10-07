package com.example.datban.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;

public record HoaDonRequest(
        @NotNull Long idDat,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal tienBan,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal tienAn,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal tongTien
) {}
