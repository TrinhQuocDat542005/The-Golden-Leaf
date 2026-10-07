package com.example.datban.controller;

import com.example.datban.dto.BookingItemRequest;
import com.example.datban.dto.BookingItemResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import com.example.datban.service.BookingItemService;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/giohang")
@CrossOrigin(origins = "*")
public class GioHangController {

    private final BookingItemService service;

    public GioHangController(BookingItemService service) {
        this.service = service;
    }

    @PostMapping("/datmon")
    public List<BookingItemResponse> datMon(
            @RequestBody @NotEmpty @Size(max = 100) List<@Valid BookingItemRequest> requests, Principal principal) {
        return service.replace(requests.get(0).idDat(), requests, principal == null ? null : principal.getName())
                .stream().map(BookingItemResponse::from).toList();
    }

    @PutMapping("/{idDat}")
    public List<BookingItemResponse> replaceCart(@PathVariable Long idDat,
            @RequestBody @Size(max = 100) List<@Valid BookingItemRequest> requests, Principal principal) {
        return service.replace(idDat, requests, principal == null ? null : principal.getName())
                .stream().map(BookingItemResponse::from).toList();
    }
}
