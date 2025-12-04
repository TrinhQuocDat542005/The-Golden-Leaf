package com.example.datban.controller;

import com.example.datban.model.ThucDon;
import com.example.datban.service.ThucDonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;

@Controller
@RequestMapping("/nhahang")
public class ThucDonController {

    @Autowired
    private ThucDonService thucDonService;

    private final String uploadDir = "uploads/";

    @GetMapping("/thuc_don")
    public String listThucDon(Model model) {
        model.addAttribute("monAnList", thucDonService.getAllMonAn());
        model.addAttribute("monAn", new ThucDon());
        return "nhahang/thuc_don";
    }

    @PostMapping("/thuc_don")
    public String createMonAn(@ModelAttribute ThucDon monAn,
                              @RequestParam("fileAnh") MultipartFile fileAnh) throws IOException {

        // Tạo thư mục nếu chưa có
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Lưu file vào thư mục uploads
        String fileName = fileAnh.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(fileAnh.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Lưu tên file vào DB
        monAn.setAnh(fileName);
        thucDonService.createMonAn(monAn);

        return "redirect:/nhahang/thuc_don";
    }
}
