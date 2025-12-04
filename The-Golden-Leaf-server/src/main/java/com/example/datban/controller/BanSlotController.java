package com.example.datban.controller;

import com.example.datban.model.BanSlot;
import com.example.datban.service.BanSlotService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/ban-slot")
public class BanSlotController {

    private final BanSlotService service;

    public BanSlotController(BanSlotService service) {
        this.service = service;
    }

    @GetMapping
    public List<BanSlot> getAllSlots() {
        return service.getAllSlots();
    }

    @PostMapping("/dat")
    public BanSlot datBan(
            @RequestParam LocalDate ngay,
            @RequestParam String khungGio,
            @RequestParam int soLuongKhach
    ) {
        return service.datBan(ngay, khungGio, soLuongKhach);
    }

    @PostMapping("/tra")
    public BanSlot traBan(
            @RequestParam LocalDate ngay,
            @RequestParam String khungGio,
            @RequestParam int soLuongKhach
    ) {
        return service.traBan(ngay, khungGio, soLuongKhach);
    }
}
