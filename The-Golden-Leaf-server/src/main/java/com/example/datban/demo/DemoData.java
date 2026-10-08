package com.example.datban.demo;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Synthetic seed in a guarded, new in-memory database only. Never a Flyway production migration. */
@Component
@Profile("demo")
@Order(0)
public class DemoData implements ApplicationRunner {
    private final JdbcTemplate db;
    private final Clock clock;
    public DemoData(JdbcTemplate db, Clock clock) { this.db = db; this.clock = clock; }
    @Override public void run(ApplicationArguments args) {
        DemoTokenVerifier.PERSONAS.forEach((persona, identity) -> {
            db.update("INSERT INTO users(uid,email,display_name,firebase_provider) VALUES(?,?,?,'portfolio')",
                    identity.uid(), identity.email(), identity.name());
            db.update("INSERT INTO user_roles(user_uid,role_id) SELECT ?,id FROM roles WHERE code='CUSTOMER'", identity.uid());
            if (persona.equals("STAFF") || persona.equals("ADMIN")) {
                db.update("INSERT INTO user_roles(user_uid,role_id) SELECT ?,id FROM roles WHERE code=?", identity.uid(), persona);
            }
        });
        seedMenu("KHAI_VI", "Salad vườn xanh", 70000, "Rau theo mùa, sốt chanh nhẹ.");
        seedMenu("KHAI_VI", "Súp nấm", 55000, "Nấm tươi và bánh mì giòn.");
        seedMenu("MON_CHINH", "Cá nướng thảo mộc", 185000, "Món chính mẫu trong bộ dữ liệu portfolio.");
        seedMenu("MON_CHINH", "Bò áp chảo", 245000, "Phục vụ cùng rau củ nướng.");
        seedMenu("TRANG_MIENG", "Panna cotta", 65000, "Kem sữa và sốt trái cây.");
        seedMenu("TRANG_MIENG", "Trái cây theo mùa", 45000, "Đĩa trái cây dùng chung.");
        for (int i = 1; i <= 4; i++) {
            db.update("INSERT INTO restaurant_tables(area_id,code,capacity) SELECT id,?,8 FROM restaurant_areas WHERE code='INDOOR'", "DEMO-0" + i);
        }
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of("Asia/Ho_Chi_Minh")));
        for (int day = 0; day < 7; day++) {
            for (String slot : List.of("07:00-11:00", "11:00-15:00", "15:00-19:00", "19:00-23:00")) {
                db.update("INSERT INTO time_slots(booking_date,slot_label,initial_table_count,remaining_table_count) VALUES(?,?,4,4)", today.plusDays(day), slot);
            }
        }
        db.update("INSERT INTO bookings(user_uid,customer_email,customer_name,booking_date,slot_label,guest_count,preferred_area_name,status,reserved_tables,idempotency_key,note) VALUES('demo-customer','customer@example.invalid','Khách demo',?,'19:00-23:00',4,'Trong nhà','ASSIGNED',1,'demo-seed-today','Đơn mẫu đã đối soát giả lập, dùng thử nhận khách/hoàn tất.')", today);
        Long id = db.queryForObject("SELECT id FROM bookings WHERE idempotency_key='demo-seed-today'", Long.class);
        db.update("UPDATE time_slots SET remaining_table_count=3 WHERE booking_date=? AND slot_label='19:00-23:00'", today);
        db.update("INSERT INTO booking_tables(booking_id,table_id) SELECT ?,id FROM restaurant_tables WHERE code='DEMO-01'", id);
        db.update("INSERT INTO invoices(booking_id,table_fee,food_total,grand_total) VALUES(?,200000,0,200000)", id);
        db.update("INSERT INTO payments(booking_id,provider,provider_reference,amount,status,bank_name,account_number,account_name,transfer_reference,paid_at) VALUES(?,'BANK_TRANSFER',?,200000,'PAID','DEMO - KHONG CHUYEN TIEN','NOT-A-REAL-ACCOUNT','PORTFOLIO FIXTURE ONLY',?,CURRENT_TIMESTAMP)", id, "TGL" + id, "DEMO-SEED-RECEIPT");
        db.update("INSERT INTO notifications(user_uid,booking_id,type,title,message) VALUES('demo-customer',?,'DEMO','Chào mừng bản demo','Dữ liệu giả lập. Không chuyển tiền, không gửi FCM. Dừng backend để đặt lại demo.')", id);
    }
    private void seedMenu(String category, String name, int price, String description) {
        db.update("INSERT INTO menu_items(category_id,name,price,description,legacy_group) SELECT id,?,?,?,code FROM menu_categories WHERE code=?", name, price, description, category);
    }
}
