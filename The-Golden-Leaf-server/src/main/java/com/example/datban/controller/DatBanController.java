package com.example.datban.controller;

import com.example.datban.dto.BookingCreateRequest;
import com.example.datban.dto.BookingResponse;
import com.example.datban.model.DatBan;
import com.example.datban.service.DatBanService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/datban")
@CrossOrigin(origins = "*")
public class DatBanController {

    private final DatBanService datBanService;

    public DatBanController(DatBanService datBanService) {
        this.datBanService = datBanService;
    }

    @PostMapping("/save")
    public BookingResponse saveDatBan(@Valid @RequestBody BookingCreateRequest request) {
        DatBan booking = new DatBan(null, request.email(), request.ten(), request.ngay(), request.khungGio(),
                request.soLuong(), request.ghiChu(), request.viTriBan());
        return BookingResponse.from(datBanService.saveDatBan(booking));
    }

    @GetMapping("/latest")
    public BookingResponse getLatestDatBan(Principal principal) {
        DatBan booking = principal == null
                ? datBanService.getLatestDatBan()
                : datBanService.getLatestDatBan(principal.getName());
        return BookingResponse.from(booking);
    }
}
