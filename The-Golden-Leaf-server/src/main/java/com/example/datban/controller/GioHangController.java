package com.example.datban.controller;

import com.example.datban.dto.BookingItemRequest;
import com.example.datban.dto.BookingItemResponse;
import com.example.datban.exception.ResourceNotFoundException;
import com.example.datban.model.DatBan;
import com.example.datban.model.GioHang;
import com.example.datban.repository.DatBanRepository;
import com.example.datban.repository.GioHangRepository;
import com.example.datban.repository.ThucDonRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/giohang")
@CrossOrigin(origins = "*")
public class GioHangController {

    private final GioHangRepository gioHangRepository;
    private final DatBanRepository datBanRepository;
    private final ThucDonRepository thucDonRepository;

    public GioHangController(GioHangRepository gioHangRepository, DatBanRepository datBanRepository,
                             ThucDonRepository thucDonRepository) {
        this.gioHangRepository = gioHangRepository;
        this.datBanRepository = datBanRepository;
        this.thucDonRepository = thucDonRepository;
    }

    @PostMapping("/datmon")
    public List<BookingItemResponse> datMon(@NotEmpty @Valid @RequestBody List<BookingItemRequest> requests) {
        List<GioHang> entities = requests.stream().map(request -> {
            DatBan booking = datBanRepository.findById(request.idDat())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt đặt bàn"));
            var menuItem = thucDonRepository.findById(request.idThucDon())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy món ăn"));
            return new GioHang(request.idDat(), booking.getEmail(), request.idThucDon(), menuItem.getTenMon(),
                    request.soLuong(), menuItem.getGia());
        }).toList();
        return gioHangRepository.saveAll(entities).stream().map(BookingItemResponse::from).toList();
    }
}
