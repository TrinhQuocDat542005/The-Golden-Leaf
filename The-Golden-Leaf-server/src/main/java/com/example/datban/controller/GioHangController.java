package com.example.datban.controller;

import com.example.datban.model.GioHang;
import com.example.datban.repository.GioHangRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.example.datban.model.DatBan;
import com.example.datban.repository.DatBanRepository;

import java.util.List;

@RestController
@RequestMapping("/api/giohang")
@CrossOrigin(origins = "*")
public class GioHangController {

    @Autowired
    private GioHangRepository gioHangRepository;
@Autowired
private DatBanRepository datBanRepository;

@PostMapping("/datmon")
public String datMon(@RequestBody List<GioHang> danhSachMon) {
    try {
        for (GioHang gioHang : danhSachMon) {
            // Lấy DatBan theo idDat
            DatBan datBan = datBanRepository.findById(gioHang.getIdDat())
                               .orElseThrow(() -> new RuntimeException("DatBan không tồn tại"));

            // Gán email cho GioHang
            gioHang.setEmail(datBan.getEmail());
        }
        // Lưu tất cả món
        gioHangRepository.saveAll(danhSachMon);
        return "Đặt món thành công!";
    } catch (Exception e) {
        e.printStackTrace();
        return "Lỗi khi gửi đặt món: " + e.getMessage();
    }
}}