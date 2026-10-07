package com.example.datban.controller;

import com.example.datban.dto.HoaDonRequest;
import com.example.datban.dto.InvoiceResponse;
import com.example.datban.service.HoaDonService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hoadon")
@CrossOrigin(origins = "*")
public class HoaDonController {

    private final HoaDonService service;

    public HoaDonController(HoaDonService service) {
        this.service = service;
    }

    @PostMapping("/create")
    public InvoiceResponse createHoaDon(@Valid @RequestBody HoaDonRequest req) {
        return InvoiceResponse.from(service.saveHoaDon(req));
    }
}
