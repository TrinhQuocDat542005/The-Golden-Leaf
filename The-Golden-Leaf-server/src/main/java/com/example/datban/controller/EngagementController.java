package com.example.datban.controller;

import com.example.datban.service.EngagementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
public class EngagementController {
    private final EngagementService service;
    public EngagementController(EngagementService service) { this.service=service; }
    public record ReviewRequest(@NotNull @Positive Long thucDonId,
            @NotBlank @Size(max=2000) String noiDung, @NotNull @Min(1) @Max(5) Integer rating) {}
    @GetMapping("/api/yeu-thich/list") public Object favorites() { return service.favorites(); }
    @PostMapping("/api/yeu-thich/add") public void add(@RequestParam @Positive Long idThucDon) { service.favorite(idThucDon,true); }
    @DeleteMapping("/api/yeu-thich/remove") public void remove(@RequestParam @Positive Long idThucDon) { service.favorite(idThucDon,false); }
    @GetMapping("/api/binhluan/{id}") public Object reviews(@PathVariable @Positive Long id) { return service.reviews(id); }
    @PostMapping("/api/binhluan/add") public Object review(@Valid @RequestBody ReviewRequest request) {
        return service.review(request.thucDonId(),request.noiDung(),request.rating());
    }
}
