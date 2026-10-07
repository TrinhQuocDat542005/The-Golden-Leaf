package com.example.datban.controller;

import com.example.datban.service.*;
import com.example.datban.security.RestaurantPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

@RestController
public class NotificationController {
    private final NotificationService service;
    private final JdbcTemplate db;
    private final BookingEvents events;
    public NotificationController(NotificationService service,JdbcTemplate db,BookingEvents events) { this.service=service;this.db=db;this.events=events; }
    @GetMapping("/api/notifications") public Object list() { return service.list(RestaurantPrincipal.required().uid()); }
    @GetMapping("/api/notifications/unread-count") public Object count() { return java.util.Map.of("count",service.unread(RestaurantPrincipal.required().uid())); }
    @PostMapping("/api/notifications/{id}/read") public void read(@PathVariable Long id) { service.read(RestaurantPrincipal.required().uid(),id); }
    public record Device(@NotBlank @Size(max=512) String token) {}
    @PostMapping("/api/devices") public void register(@Valid @RequestBody Device req) { service.register(RestaurantPrincipal.required().uid(),req.token()); }
    @DeleteMapping("/api/devices") public void unregister(@Valid @RequestBody Device req) { service.unregister(RestaurantPrincipal.required().uid(),req.token()); }
    @GetMapping("/api/staff/deliveries") public Object deliveries() {
        return OperationsService.rows(db.queryForList("SELECT id,notification_id,status,attempts,next_attempt_at,last_error FROM notification_deliveries ORDER BY id DESC LIMIT 200"));
    }
    @org.springframework.transaction.annotation.Transactional
    @PostMapping("/api/staff/deliveries/{id}/retry") public void retry(@PathVariable Long id) {
        if (db.update("UPDATE notification_deliveries SET status='PENDING',attempts=0,next_attempt_at=CURRENT_TIMESTAMP,last_error=NULL WHERE id=? AND status='FAILED'",id)!=1)
            throw new com.example.datban.exception.BusinessRuleException("INVALID_DELIVERY_STATE","Chỉ thử lại thông báo đã lỗi");
        events.audit("RETRY_DELIVERY","DELIVERY",id,"FAILED","PENDING");
    }
}
