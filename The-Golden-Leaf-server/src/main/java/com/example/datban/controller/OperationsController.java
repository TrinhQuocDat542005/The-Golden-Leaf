package com.example.datban.controller;

import com.example.datban.service.*;
import com.example.datban.model.DatBan;
import com.example.datban.security.RestaurantPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;
import static com.example.datban.service.OperationsService.rows;

@RestController
public class OperationsController {
    private final OperationsService ops;
    private final HoaDonService invoices;
    private final JdbcTemplate db;
    private final InventoryService inventory;
    private final BookingLifecycleService lifecycle;
    public OperationsController(OperationsService ops,HoaDonService invoices,JdbcTemplate db,InventoryService inventory,BookingLifecycleService lifecycle) { this.ops=ops;this.invoices=invoices;this.db=db;this.inventory=inventory;this.lifecycle=lifecycle; }
    @GetMapping("/api/payments/bookings/{id}/quote")
    public Object quote(@PathVariable Long id) { return invoices.preview(id,RestaurantPrincipal.required().email()); }
    @PostMapping("/api/payments/bookings/{id}")
    public Object create(@PathVariable Long id) { return ops.payment(id,RestaurantPrincipal.required().email(),true); }
    @GetMapping("/api/payments/bookings/{id}")
    public Object payment(@PathVariable Long id) { return ops.payment(id,RestaurantPrincipal.required().email(),false); }
    public record Transfer(@NotBlank @Size(max=128) String reference,@NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal amount) {}
    @PostMapping("/api/staff/bookings/{id}/verify-payment")
    public Object verify(@PathVariable Long id,@Valid @RequestBody Transfer transfer) { return ops.verifyPayment(id,transfer.reference(),transfer.amount()); }
    @PostMapping("/api/staff/bookings/{id}/verify-refund")
    public Object refund(@PathVariable Long id,@Valid @RequestBody Transfer transfer) { return ops.refund(id,transfer.reference(),transfer.amount()); }
    @GetMapping("/api/staff/bookings") public Object queue(@RequestParam(required=false) java.time.LocalDate date) { return ops.queue(date); }
    @GetMapping("/api/staff/allocations") public Object allocations() { return ops.allocations(); }
    @GetMapping("/api/staff/bookings/{id}") public Object detail(@PathVariable Long id) { return ops.detail(id); }
    @GetMapping("/api/staff/tables") public Object tables() { return ops.tables(); }
    @GetMapping("/api/staff/areas") public Object areas() { return rows(db.queryForList("SELECT id,name FROM restaurant_areas WHERE active=TRUE ORDER BY id")); }
    public record TableRequest(@NotNull Long areaId,@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{1,50}") String code,@Min(1) @Max(80) int capacity) {}
    @PostMapping("/api/admin/tables") public void table(@Valid @RequestBody TableRequest req) { ops.createTable(req.areaId(),req.code(),req.capacity()); }
    public record Assignment(@NotEmpty @Size(max=10) List<@NotNull @Positive Long> tableIds) {}
    @PostMapping("/api/staff/bookings/{id}/assign") public DatBan assign(@PathVariable Long id,@Valid @RequestBody Assignment req) { return ops.assign(id,req.tableIds()); }
    @PostMapping("/api/staff/bookings/{id}/check-in") public DatBan checkIn(@PathVariable Long id) { return ops.advance(id,false); }
    @PostMapping("/api/staff/bookings/{id}/complete") public DatBan complete(@PathVariable Long id) { return ops.advance(id,true); }
    @PostMapping("/api/staff/bookings/{id}/cancel") public DatBan cancel(@PathVariable Long id) { return lifecycle.cancel(id,null); }
    @PostMapping("/api/admin/inventory/reconcile") public Object reconcile() { return Map.of("physicalTableCount",inventory.reconcile()); }
    public record RoleRequest(@NotBlank @Size(max=128) String uid,@NotBlank String role,@NotNull Boolean grant) {}
    @PostMapping("/api/admin/roles") public void role(@Valid @RequestBody RoleRequest req) { ops.role(req.uid(),req.role(),req.grant()); }
    @GetMapping("/api/admin/audit") public Object audit() { return rows(db.queryForList("SELECT * FROM audit_logs ORDER BY id DESC LIMIT 200")); }
    public record UserStatus(@NotNull Boolean active) {}
    @PutMapping("/api/admin/users/{uid}/status") public void status(@PathVariable String uid,@Valid @RequestBody UserStatus req) { ops.userStatus(uid,req.active()); }
    @GetMapping("/api/admin/users") public Object users() { return rows(db.queryForList("SELECT uid,email,display_name,status FROM users ORDER BY created_at DESC LIMIT 200")); }
    @GetMapping("/api/staff/refunds") public Object refunds() { return rows(db.queryForList("SELECT booking_id,amount,status FROM payments WHERE status='REFUND_REQUIRED' ORDER BY id LIMIT 200")); }
}
