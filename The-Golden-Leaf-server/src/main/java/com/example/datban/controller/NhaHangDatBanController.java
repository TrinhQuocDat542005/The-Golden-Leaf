// package com.example.datban.controller;

// import org.springframework.stereotype.Controller;
// import org.springframework.ui.Model;
// import org.springframework.web.bind.annotation.GetMapping;
// import com.example.datban.dto.DatBanInfoDTO;
// import com.example.datban.service.DatBanService;
// import java.util.List;
// import org.springframework.beans.factory.annotation.Autowired;

// @Controller
// public class NhaHangDatBanController {

//     @Autowired
//     private DatBanService datBanService;

//     @GetMapping("/nhahang/danh-sach-dat-ban")
//     public String danhSachDatBan(Model model) {
//         List<DatBanInfoDTO> danhSach = datBanService.getDanhSachDatBanDaThanhToan();

//         // Không cần gọi banSlots nữa
//         model.addAttribute("danhSachDatBan", danhSach);
//         return "nhahang/danh-sach-dat-ban"; // trỏ tới file HTML vừa chỉnh
//     }
// }
