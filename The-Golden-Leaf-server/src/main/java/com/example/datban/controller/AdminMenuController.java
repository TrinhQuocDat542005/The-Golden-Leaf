package com.example.datban.controller;

import com.example.datban.model.*;
import com.example.datban.repository.ThucDonRepository;
import com.example.datban.service.*;
import com.example.datban.exception.ResourceNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.io.IOException;

@RestController
@RequestMapping("/api/admin/menu")
public class AdminMenuController {
    private final ThucDonRepository menu;
    private final BookingEvents events;
    private final ImageStorage images;
    private final org.springframework.core.env.Environment environment;
    public AdminMenuController(ThucDonRepository menu,BookingEvents events,ImageStorage images,
            org.springframework.core.env.Environment environment) {this.menu=menu;this.events=events;this.images=images;this.environment=environment;}
    public record Item(@NotBlank @Size(max=255) String tenMon,@NotNull @DecimalMin("0") @Digits(integer=10,fraction=2) BigDecimal gia,
            @Size(max=5000) String moTa,@Size(max=512) @Pattern(regexp="^(/uploads/[A-Za-z0-9.-]+|https://[^\\s]+)?$") String anh,@NotNull NhomMon nhom,boolean active) {}
    @GetMapping public Object list(){return menu.findAll();}
    @PostMapping @Transactional public Object create(@Valid @RequestBody Item req){return save(new ThucDon(),req);}
    @PutMapping("/{id}") @Transactional public Object update(@PathVariable Long id,@Valid @RequestBody Item req){return save(menu.findById(id).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy món")),req);}
    private ThucDon save(ThucDon item,Item req){
        String old=item.getIdThucDon()==null ? null : item.getTenMon()+":"+item.getGia()+":"+item.isActive();
        item.setTenMon(req.tenMon());item.setGia(req.gia());item.setMoTa(req.moTa());item.setAnh(req.anh());item.setNhom(req.nhom());item.setActive(req.active());
        menu.saveAndFlush(item);events.audit("SAVE_MENU","MENU",item.getIdThucDon(),old,req.tenMon()+":"+req.gia()+":"+req.active());return item;
    }
    @PostMapping("/image") public Object image(@RequestParam("image") MultipartFile file) throws IOException {
        if (environment.matchesProfiles("demo")) throw new com.example.datban.exception.BusinessRuleException("DEMO_UPLOAD_DISABLED","Demo không đọc/ghi thư mục uploads của môi trường thật.");
        return java.util.Map.of("url",images.store(file));
    }
}
