package com.example.datban.controller;

import com.example.datban.dto.BookingCreateRequest;
import com.example.datban.dto.BookingResponse;
import com.example.datban.model.DatBan;
import com.example.datban.service.DatBanService;
import com.example.datban.service.BookingLifecycleService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/datban")
@CrossOrigin(origins = "*")
public class DatBanController {

    private final DatBanService datBanService;
    private final BookingLifecycleService lifecycle;

    public DatBanController(DatBanService datBanService, BookingLifecycleService lifecycle) {
        this.datBanService = datBanService;
        this.lifecycle = lifecycle;
    }

    @PostMapping("/save")
    public BookingResponse saveDatBan(@Valid @RequestBody BookingCreateRequest request,
            @RequestHeader("Idempotency-Key") String key, Principal principal) {
        DatBan booking = new DatBan(null, request.email(), request.ten(), request.ngay(), request.khungGio(),
                request.soLuong(), request.ghiChu(), request.viTriBan());
        return BookingResponse.from(lifecycle.create(booking, key, principal == null ? null : principal.getName()));
    }

    @PostMapping("/{id}/confirm")
    public BookingResponse confirm(@PathVariable Long id, Principal principal) {
        return BookingResponse.from(lifecycle.confirm(id, principal == null ? null : principal.getName()));
    }

    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(@PathVariable Long id, Principal principal) {
        return BookingResponse.from(lifecycle.cancel(id, principal == null ? null : principal.getName()));
    }

    @GetMapping("/latest")
    public BookingResponse getLatestDatBan(Principal principal) {
        DatBan booking = principal == null
                ? datBanService.getLatestDatBan()
                : datBanService.getLatestDatBan(principal.getName());
        return BookingResponse.from(booking);
    }

    @GetMapping("/{id}")
    public BookingResponse getBooking(@PathVariable Long id, Principal principal) {
        return BookingResponse.from(datBanService.getById(id, principal == null ? null : principal.getName()));
    }
}
