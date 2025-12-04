package com.example.datban.controller;

import com.example.datban.dto.HoaDonRequest;
import com.example.datban.model.HoaDon;
import com.example.datban.service.HoaDonService;
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
    public HoaDon createHoaDon(@RequestBody HoaDonRequest req) {
        return service.saveHoaDon(req);
    }
}
