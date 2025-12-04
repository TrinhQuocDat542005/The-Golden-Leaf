package com.example.datban.controller;

import com.example.datban.model.DatBan;
import com.example.datban.service.DatBanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/api/datban")
@CrossOrigin(origins = "*")
public class DatBanController {

    @Autowired
    private DatBanService datBanService;

    @PostMapping("/save")
    public DatBan saveDatBan(@RequestBody DatBan datBan) {
        // Lấy email từ datBan hoặc tự động set nếu có authentication
        datBanService.saveDatBan(datBan);
        return datBan; // trả về JSON cho client
    }
 @GetMapping("/latest")
public DatBan getLatestDatBan(Principal principal) {
    // bắt buộc phải có principal
    return datBanService.getLatestDatBan(principal.getName());
}





}
