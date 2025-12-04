package com.example.datban.controller.api;

import com.example.datban.model.ThucDon;
import com.example.datban.service.ThucDonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/thucdon")
public class ThucDonApiController {

    @Autowired
    private ThucDonService thucDonService;

    // Lấy toàn bộ danh sách món ăn
    @GetMapping
    public List<ThucDon> getAllThucDon() {
        return thucDonService.getAllMonAn();
    }

    // Lấy món ăn theo ID
    @GetMapping("/{id}")
    public ThucDon getThucDonById(@PathVariable Long id) {
        return thucDonService.getMonAnById(id);
    }
}
