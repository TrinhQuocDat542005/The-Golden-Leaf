package com.example.datban.controller;

import com.example.datban.dto.TimeSlotResponse;
import com.example.datban.service.BanSlotService;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/ban-slot")
@Validated
public class BanSlotController {

    private final BanSlotService service;

    public BanSlotController(BanSlotService service) {
        this.service = service;
    }

    @GetMapping
    public List<TimeSlotResponse> getAllSlots() {
        return service.getAllSlots().stream().map(TimeSlotResponse::from).toList();
    }

    @PostMapping("/dat")
    public TimeSlotResponse datBan(
            @RequestParam LocalDate ngay,
            @RequestParam String khungGio,
            @RequestParam @Min(1) int soLuongKhach
    ) {
        return TimeSlotResponse.from(service.datBan(ngay, khungGio, soLuongKhach));
    }

    @PostMapping("/tra")
    public TimeSlotResponse traBan(
            @RequestParam LocalDate ngay,
            @RequestParam String khungGio,
            @RequestParam @Min(1) int soLuongKhach
    ) {
        return TimeSlotResponse.from(service.traBan(ngay, khungGio, soLuongKhach));
    }
}
