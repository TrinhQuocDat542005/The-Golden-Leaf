package com.example.datban.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record HoaDonRequest(
        @NotNull Long idDat,
        @NotNull @DecimalMin("0.00") BigDecimal tienBan,
        @NotNull @DecimalMin("0.00") BigDecimal tienAn,
        @NotNull @DecimalMin("0.00") BigDecimal tongTien
) {}
