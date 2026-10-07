package com.example.datban.controller;

import com.example.datban.model.ThucDon;
import com.example.datban.service.ThucDonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@Controller
@RequestMapping("/nhahang")
public class ThucDonController {

    @Autowired
    private ThucDonService thucDonService;
    @Autowired
    private com.example.datban.service.ImageStorage images;


    @GetMapping("/thuc_don")
    public String listThucDon(Model model) {
        model.addAttribute("monAnList", thucDonService.getAllMonAn());
        model.addAttribute("monAn", new ThucDon());
        return "nhahang/thuc_don";
    }

    @PostMapping("/thuc_don")
    public String createMonAn(@ModelAttribute ThucDon monAn,
                              @RequestParam("fileAnh") MultipartFile fileAnh) throws IOException {

        // Lưu tên file vào DB
        monAn.setIdThucDon(null);
        if (monAn.getTenMon() == null || monAn.getTenMon().isBlank() || monAn.getTenMon().length() > 255 || monAn.getGia() == null || monAn.getGia().signum() < 0 || monAn.getGia().scale() > 2 || monAn.getGia().precision() - monAn.getGia().scale() > 10)
            throw new com.example.datban.exception.BusinessRuleException("INVALID_MENU_ITEM", "Tên món hoặc giá không hợp lệ");
        monAn.setAnh(images.store(fileAnh));
        thucDonService.createMonAn(monAn);

        return "redirect:/nhahang/thuc_don";
    }
}
