package com.example.datban.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record BookingItemRequest(
        @NotNull Long idDat,
        @NotNull Long idThucDon,
        @NotBlank @Size(max = 255) String tenMon,
        @NotNull @Min(1) @Max(100) Integer soLuong,
        @NotNull @DecimalMin("0.00") BigDecimal giaMon
) {}
