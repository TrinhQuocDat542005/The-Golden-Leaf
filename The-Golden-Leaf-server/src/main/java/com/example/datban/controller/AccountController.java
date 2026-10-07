package com.example.datban.controller;

import com.example.datban.service.*;
import com.example.datban.repository.*;
import com.example.datban.security.RestaurantPrincipal;
import com.example.datban.model.DatBan;
import com.example.datban.exception.ResourceNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;

@RestController
public class AccountController {
    private final JdbcTemplate db;
    private final DatBanRepository bookings;
    private final GioHangRepository items;
    private final BookingLifecycleService lifecycle;
    public AccountController(JdbcTemplate db,DatBanRepository bookings,GioHangRepository items,BookingLifecycleService lifecycle) { this.db=db;this.bookings=bookings;this.items=items;this.lifecycle=lifecycle; }
    @GetMapping("/api/taikhoan/choXacNhan") public Object pending() { return list(true); }
    @GetMapping("/api/taikhoan/lichSuDonDat") public Object history() { return list(false); }
    private Object list(boolean pending) {
        var p=RestaurantPrincipal.required();
        return db.queryForList("SELECT id FROM bookings WHERE (user_uid=? OR (user_uid IS NULL AND LOWER(customer_email)=LOWER(?))) AND status " +
                (pending ? "IN" : "NOT IN") + " ('HOLDING','CONFIRMED','ASSIGNED') ORDER BY id DESC LIMIT 100",Long.class,p.uid(),p.email()).stream().map(this::detail).toList();
    }
    @GetMapping("/api/dondat/{id}") public Object detail(@PathVariable Long id) {
        var p=RestaurantPrincipal.required();
        DatBan b=bookings.findById(id).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy đơn"));
        BookingLifecycleService.checkOwner(b,p.email());
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("idDat",id);result.put("email",b.getEmail());result.put("ten",b.getTen());result.put("ngay",b.getNgay().toString());
        result.put("khungGio",b.getKhungGio());result.put("soLuong",b.getSoLuong());result.put("ghiChu",b.getGhiChu());result.put("viTriBan",b.getViTriBan());result.put("status",b.getStatus());
        var tableCodes=db.queryForList("SELECT t.code FROM booking_tables bt JOIN restaurant_tables t ON t.id=bt.table_id WHERE bt.booking_id=? ORDER BY t.id",String.class,id);
        result.put("soBan",tableCodes.isEmpty() ? null : String.join(", ",tableCodes));
        var invoice=db.queryForList("SELECT * FROM invoices WHERE booking_id=?",id);
        if (!invoice.isEmpty()) { result.put("tienBan",invoice.get(0).get("table_fee"));result.put("tienAn",invoice.get(0).get("food_total"));result.put("tongTien",invoice.get(0).get("grand_total")); }
        var payments=db.queryForList("SELECT status,paid_at FROM payments WHERE booking_id=? AND provider='BANK_TRANSFER' ORDER BY id",id);
        if (!payments.isEmpty()) {result.put("paymentStatus",payments.get(0).get("status"));result.put("ngayGioThanhToan",payments.get(0).get("paid_at")==null ? null : payments.get(0).get("paid_at").toString());}
        result.put("danhSachMon",items.findByIdDat(id));return result;
    }
    @DeleteMapping("/api/taikhoan/huyDon/{id}") public void cancel(@PathVariable Long id) { lifecycle.cancel(id,RestaurantPrincipal.required().email()); }
}
