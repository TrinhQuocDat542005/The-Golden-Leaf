package com.example.datban.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record BookingCreateRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(max = 255) String ten,
        @NotNull @FutureOrPresent LocalDate ngay,
        @NotBlank @Size(max = 32) String khungGio,
        @NotNull @Min(1) @Max(80) Integer soLuong,
        @Size(max = 1000) String ghiChu,
        @NotBlank @Size(max = 100) String viTriBan
) {}
