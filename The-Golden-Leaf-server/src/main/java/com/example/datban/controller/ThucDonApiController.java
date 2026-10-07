package com.example.datban.controller.api;

import com.example.datban.dto.MenuItemResponse;
import com.example.datban.service.ThucDonService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/thucdon")
public class ThucDonApiController {

    private final ThucDonService thucDonService;

    public ThucDonApiController(ThucDonService thucDonService) {
        this.thucDonService = thucDonService;
    }

    @GetMapping
    public List<MenuItemResponse> getAllThucDon() {
        return thucDonService.getAllMonAn().stream().filter(com.example.datban.model.ThucDon::isActive).map(MenuItemResponse::from).toList();
    }

    @GetMapping("/{id}")
    public MenuItemResponse getThucDonById(@PathVariable Long id) {
        var item = thucDonService.getMonAnById(id);
        if (!item.isActive()) throw new com.example.datban.exception.ResourceNotFoundException("Không tìm thấy món đang phục vụ");
        return MenuItemResponse.from(item);
    }
}
