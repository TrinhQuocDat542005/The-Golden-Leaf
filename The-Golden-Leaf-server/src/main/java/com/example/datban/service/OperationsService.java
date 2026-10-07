package com.example.datban.service;

import com.example.datban.model.*;
import com.example.datban.repository.*;
import com.example.datban.security.RestaurantPrincipal;
import com.example.datban.exception.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.*;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class OperationsService {
    private final JdbcTemplate db;
    private final BookingLifecycleService lifecycle;
    private final HoaDonService invoices;
    private final BanSlotRepository slots;
    private final BookingEvents events;
    private final Clock clock;
    private final java.time.ZoneId zone;
    private final String bank, account, accountName;
    public OperationsService(JdbcTemplate db, BookingLifecycleService lifecycle, HoaDonService invoices,
            BanSlotRepository slots, BookingEvents events, Clock clock,
            @Value("${app.payment.bank-name:}") String bank,
            @Value("${app.payment.account-number:}") String account,
            @Value("${app.payment.account-name:}") String accountName,
            @Value("${app.booking.zone:Asia/Ho_Chi_Minh}") String zone) {
        this.db=db; this.lifecycle=lifecycle; this.invoices=invoices; this.slots=slots;
        this.events=events; this.clock=clock; this.bank=bank; this.account=account; this.accountName=accountName;
        this.zone=java.time.ZoneId.of(zone);
    }
    public Map<String,Object> payment(Long id, String email, boolean create) {
        var booking=lifecycle.lock(id,email);
        var payments=db.queryForList("SELECT * FROM payments WHERE booking_id=? AND provider='BANK_TRANSFER' ORDER BY id",id);
        if (!payments.isEmpty()) return paymentView(payments.get(0));
        if (!create) throw new ResourceNotFoundException("Chưa có yêu cầu thanh toán");
        if (!Set.of(BookingStatus.CONFIRMED,BookingStatus.ASSIGNED).contains(booking.getStatus())) fail("INVALID_BOOKING_STATE", "Đơn chưa xác nhận hoặc đã đóng");
        if (bank.isBlank() || account.isBlank() || accountName.isBlank()) fail("PAYMENT_NOT_CONFIGURED", "Nhà hàng chưa cấu hình tài khoản nhận tiền");
        var invoice=invoices.ensureInvoice(id,email);
        db.update("INSERT INTO payments(booking_id,provider,provider_reference,amount,status,bank_name,account_number,account_name) VALUES(?,'BANK_TRANSFER',?,?,'PENDING',?,?,?)",
                id,"TGL"+id,invoice.getTongTien(),bank,account,accountName);
        events.emit(booking,"PAYMENT_PENDING","Chuyển khoản đúng nội dung TGL"+id+". Nhân viên sẽ kiểm tra giao dịch thực tế.");
        events.audit("CREATE_PAYMENT","BOOKING",id,null,"PENDING");
        return payment(id,email,false);
    }
    private Map<String,Object> paymentView(Map<String,Object> row) {
        var result=new LinkedHashMap<String,Object>();
        result.put("id",row.get("id")); result.put("bookingId",row.get("booking_id"));
        result.put("amount",row.get("amount")); result.put("currency",row.get("currency"));
        result.put("status",row.get("status")); result.put("reference",row.get("provider_reference"));
        result.put("bankName",row.get("bank_name") == null ? "" : row.get("bank_name"));
        result.put("accountNumber",row.get("account_number") == null ? "" : row.get("account_number"));
        result.put("accountName",row.get("account_name") == null ? "" : row.get("account_name"));
        result.put("paidAt",row.get("paid_at") instanceof Timestamp stamp ? stamp.toInstant().toString() : null);
        return result;
    }
    public Map<String,Object> verifyPayment(Long id, String reference, BigDecimal received) {
        var booking=lifecycle.lock(id,null);
        reference=reference(reference);
        var row=paymentRow(id);
        if (received.compareTo((BigDecimal)row.get("amount"))!=0) fail("AMOUNT_MISMATCH","Số tiền thực nhận không khớp hóa đơn");
        if (row.get("transfer_reference")!=null) {
            if (!reference.equals(row.get("transfer_reference"))) fail("PAYMENT_CONFLICT","Đơn đã ghi nhận giao dịch khác");
            return paymentView(row);
        }
        boolean late=booking.getStatus()==BookingStatus.CANCELLED && "CANCELLED".equals(row.get("status"));
        if (!late && (!"PENDING".equals(row.get("status")) || !Set.of(BookingStatus.CONFIRMED,BookingStatus.ASSIGNED,BookingStatus.SEATED).contains(booking.getStatus())))
            fail("INVALID_PAYMENT_STATE","Không thể ghi nhận thanh toán cho đơn đã đóng");
        db.update("UPDATE payments SET status=?, transfer_reference=?, paid_at=?, updated_at=? WHERE id=?",
                late ? "REFUND_REQUIRED" : "PAID",reference,Timestamp.from(clock.instant()),Timestamp.from(clock.instant()),row.get("id"));
        events.audit(late ? "VERIFY_LATE_TRANSFER" : "VERIFY_TRANSFER","PAYMENT",row.get("id"),row.get("status").toString(),reference);
        events.emit(booking,late ? "LATE_TRANSFER" : "PAID",late ? "Đơn #"+id+" đã hủy nhưng có tiền đến muộn; nhà hàng sẽ đối soát hoàn tiền." : "Nhà hàng đã xác nhận thanh toán đơn #"+id+".");
        return paymentView(paymentRow(id));
    }
    public Map<String,Object> refund(Long id, String reference, BigDecimal amount) {
        var booking=lifecycle.lock(id,null); var row=paymentRow(id); reference=reference(reference);
        if (amount.compareTo((BigDecimal)row.get("amount"))!=0) fail("AMOUNT_MISMATCH","Phải hoàn đúng số tiền đã thu");
        if ("REFUNDED".equals(row.get("status"))) {
            if (!reference.equals(row.get("refund_reference"))) fail("PAYMENT_CONFLICT","Đã ghi nhận hoàn tiền khác");
            return paymentView(row);
        }
        if (booking.getStatus()!=BookingStatus.CANCELLED || !"REFUND_REQUIRED".equals(row.get("status")))
            fail("INVALID_PAYMENT_STATE","Đơn không chờ hoàn tiền");
        db.update("UPDATE payments SET status='REFUNDED',refund_reference=?,updated_at=? WHERE id=?",reference,Timestamp.from(clock.instant()),row.get("id"));
        events.audit("VERIFY_REFUND","PAYMENT",row.get("id"),"REFUND_REQUIRED",reference);
        events.emit(booking,"REFUNDED","Nhà hàng đã ghi nhận hoàn tiền đơn #"+id+".");
        return paymentView(paymentRow(id));
    }
    private Map<String,Object> paymentRow(Long id) {
        return db.queryForList("SELECT * FROM payments WHERE booking_id=? AND provider='BANK_TRANSFER' FOR UPDATE",id)
                .stream().findFirst().orElseThrow(()->new ResourceNotFoundException("Chưa có yêu cầu thanh toán"));
    }
    private String reference(String value) {
        if (value==null || !value.trim().matches("[A-Za-z0-9_-]{4,128}")) fail("INVALID_TRANSFER_REFERENCE","Mã giao dịch phải có 4–128 ký tự chữ, số, _ hoặc -");
        return value.trim().toUpperCase(Locale.ROOT);
    }
    public List<Map<String,Object>> tables() {
        return rows(db.queryForList("SELECT t.id,t.code,t.capacity,t.active,a.name AS area FROM restaurant_tables t JOIN restaurant_areas a ON a.id=t.area_id ORDER BY t.id"));
    }
    public void createTable(Long areaId, String code, int capacity) {
        if (!code.matches("[A-Za-z0-9_-]{1,50}") || capacity<1 || capacity>80) fail("INVALID_TABLE","Mã bàn hoặc sức chứa không hợp lệ");
        if (db.queryForObject("SELECT COUNT(*) FROM restaurant_areas WHERE id=? AND active=TRUE",Integer.class,areaId)!=1) fail("INVALID_AREA","Khu vực không hoạt động");
        db.update("INSERT INTO restaurant_tables(area_id,code,capacity) VALUES(?,?,?)",areaId,code,capacity);
        events.audit("CREATE_TABLE","TABLE",code,null,"capacity="+capacity);
    }
    public DatBan assign(Long id, List<Long> ids) {
        var booking=lifecycle.lock(id,null);
        if (ids==null || ids.size()!=booking.getReservedTables() || new HashSet<>(ids).size()!=ids.size()) fail("INVALID_TABLES","Phải chọn đúng số bàn giữ chỗ, không trùng bàn");
        var selected=ids.stream().sorted().toList();
        var current=db.queryForList("SELECT table_id FROM booking_tables WHERE booking_id=? AND released_at IS NULL ORDER BY table_id",Long.class,id);
        if (booking.getStatus()==BookingStatus.ASSIGNED && current.equals(selected)) return booking;
        if (booking.getStatus()!=BookingStatus.CONFIRMED) fail("INVALID_BOOKING_STATE","Chỉ phân bàn cho đơn đã xác nhận");
        int capacity=0;
        for (Long tableId:selected) {
            var rows=db.queryForList("SELECT t.* FROM restaurant_tables t JOIN restaurant_areas a ON a.id=t.area_id WHERE t.id=? AND t.active=TRUE AND a.active=TRUE AND t.status='AVAILABLE' FOR UPDATE",tableId);
            if (rows.isEmpty()) fail("TABLE_UNAVAILABLE","Bàn không hoạt động");
            capacity+=((Number)rows.get(0).get("capacity")).intValue();
            if (db.queryForObject("SELECT COUNT(*) FROM booking_tables bt JOIN bookings b ON b.id=bt.booking_id WHERE bt.table_id=? AND bt.released_at IS NULL AND b.booking_date=? AND b.slot_label=?",Integer.class,tableId,booking.getNgay(),booking.getKhungGio())>0)
                fail("TABLE_CONFLICT","Bàn đã được phân cho đơn khác trong khung giờ");
        }
        if (capacity<booking.getSoLuong()) fail("INSUFFICIENT_SEATS","Không đủ ghế cho nhóm khách");
        for (Long tableId:selected) db.update("INSERT INTO booking_tables(booking_id,table_id) VALUES(?,?)",id,tableId);
        booking.setStatus(BookingStatus.ASSIGNED);
        events.audit("ASSIGN_TABLES","BOOKING",id,"CONFIRMED",selected.toString());
        events.emit(booking,"ASSIGNED","Đơn #"+id+" đã được nhà hàng phân bàn.");
        return booking;
    }
    public DatBan advance(Long id, boolean complete) {
        var booking=lifecycle.lock(id,null);
        BookingStatus target=complete ? BookingStatus.COMPLETED : BookingStatus.SEATED;
        if (booking.getStatus()==target) return booking;
        if (booking.getStatus()!=(complete ? BookingStatus.SEATED : BookingStatus.ASSIGNED)) fail("INVALID_BOOKING_STATE","Thứ tự xử lý: phân bàn → nhận khách → hoàn tất");
        if (!"PAID".equals(paymentRow(id).get("status"))) fail("PAYMENT_REQUIRED","Cần đối soát thanh toán trước khi nhận khách");
        if (!complete && !booking.getNgay().equals(java.time.LocalDate.now(clock.withZone(zone)))) fail("INVALID_CHECKIN_DATE","Chỉ nhận khách đúng ngày đặt bàn");
        if (complete) {
            var slot=slots.findByNgayAndKhungGio(booking.getNgay(),booking.getKhungGio()).orElseThrow();
            int remaining=slot.getSoBanConLai()+booking.getReservedTables();
            if (remaining>slot.getSoBanBanDau()) fail("CAPACITY_CONFLICT","Sức chứa không nhất quán");
            slot.setSoBanConLai(remaining);
            db.update("UPDATE booking_tables SET released_at=? WHERE booking_id=? AND released_at IS NULL",Timestamp.from(clock.instant()),id);
        }
        events.audit(complete ? "COMPLETE" : "CHECK_IN","BOOKING",id,booking.getStatus().name(),target.name());
        booking.setStatus(target); events.emit(booking,target.name(),"Đơn #"+id+": "+target.name()); return booking;
    }
    public List<Map<String,Object>> queue() {
        return rows(db.queryForList("SELECT b.id,b.customer_name,b.customer_email,b.booking_date,b.slot_label,b.guest_count,b.preferred_area_name,b.reserved_tables,b.status,p.status AS payment_status,p.amount,p.provider_reference FROM bookings b LEFT JOIN payments p ON p.booking_id=b.id AND p.provider='BANK_TRANSFER' WHERE b.status NOT IN ('CANCELLED','EXPIRED','COMPLETED') ORDER BY b.booking_date,b.slot_label,b.id LIMIT 200"));
    }
    public List<Map<String,Object>> queue(java.time.LocalDate date) {
        if(date==null)return queue();
        return rows(db.queryForList("SELECT b.id,b.customer_name,b.customer_email,b.booking_date,b.slot_label,b.guest_count,b.preferred_area_name,b.reserved_tables,b.status,p.status AS payment_status,p.amount,p.provider_reference FROM bookings b LEFT JOIN payments p ON p.booking_id=b.id AND p.provider='BANK_TRANSFER' WHERE b.booking_date=? AND b.status NOT IN ('CANCELLED','EXPIRED','COMPLETED') ORDER BY b.slot_label,b.id LIMIT 200",date));
    }
    public List<Map<String,Object>> allocations() {
        return rows(db.queryForList("SELECT bt.booking_id,bt.table_id,b.booking_date,b.slot_label FROM booking_tables bt JOIN bookings b ON b.id=bt.booking_id WHERE bt.released_at IS NULL ORDER BY b.booking_date,b.slot_label,bt.table_id LIMIT 2000"));
    }
    public Map<String,Object> detail(Long id) {
        var booking=lifecycle.lock(id,null);
        return Map.of("booking",com.example.datban.dto.BookingResponse.from(booking),
                "items",rows(db.queryForList("SELECT item_name,quantity,unit_price,line_total FROM booking_items WHERE booking_id=? ORDER BY id",id)),
                "invoices",rows(db.queryForList("SELECT table_fee,food_total,grand_total,currency FROM invoices WHERE booking_id=?",id)),
                "payments",rows(db.queryForList("SELECT amount,status,provider_reference,transfer_reference,refund_reference,paid_at FROM payments WHERE booking_id=? ORDER BY id",id)),
                "tables",rows(db.queryForList("SELECT t.code,a.name AS area,bt.assigned_at,bt.released_at FROM booking_tables bt JOIN restaurant_tables t ON t.id=bt.table_id JOIN restaurant_areas a ON a.id=t.area_id WHERE bt.booking_id=? ORDER BY t.id",id)));
    }
    public static List<Map<String,Object>> rows(List<Map<String,Object>> source) {
        return source.stream().map(row->{
            Map<String,Object> normalized=new LinkedHashMap<>();
            row.forEach((key,value)->normalized.put(key.toLowerCase(Locale.ROOT),value instanceof java.sql.Timestamp stamp ? stamp.toInstant().toString() : value instanceof java.sql.Date date ? date.toLocalDate().toString() : value));
            return normalized;
        }).toList();
    }
    public void role(String uid, String role, boolean grant) {
        if (!Set.of("STAFF","ADMIN").contains(role)) fail("INVALID_ROLE","Chỉ quản lý quyền STAFF hoặc ADMIN");
        db.queryForList("SELECT id FROM roles WHERE code='ADMIN' FOR UPDATE");
        if (db.queryForObject("SELECT COUNT(*) FROM users WHERE uid=? AND status='ACTIVE'",Integer.class,uid)!=1) fail("INVALID_USER","Tài khoản chưa đồng bộ hoặc bị khóa");
        int existing=db.queryForObject("SELECT COUNT(*) FROM user_roles ur JOIN roles r ON r.id=ur.role_id WHERE ur.user_uid=? AND r.code=?",Integer.class,uid,role);
        if (grant && existing==0) db.update("INSERT INTO user_roles(user_uid,role_id) SELECT ?,id FROM roles WHERE code=?",uid,role);
        if (!grant && existing>0) {
            if (role.equals("ADMIN") && db.queryForObject("SELECT COUNT(*) FROM user_roles ur JOIN roles r ON r.id=ur.role_id JOIN users u ON u.uid=ur.user_uid WHERE r.code='ADMIN' AND u.status='ACTIVE'",Integer.class)<=1) fail("LAST_ADMIN","Không thể gỡ quản trị viên cuối cùng");
            db.update("DELETE FROM user_roles WHERE user_uid=? AND role_id=(SELECT id FROM roles WHERE code=?)",uid,role);
        }
        events.audit("CHANGE_ROLE","USER",uid,existing>0 ? role : null,grant ? role : null);
    }
    public void userStatus(String uid, boolean active) {
        db.queryForList("SELECT id FROM roles WHERE code='ADMIN' FOR UPDATE");
        var users=db.queryForList("SELECT status FROM users WHERE uid=? FOR UPDATE",uid);
        if (users.isEmpty()) throw new ResourceNotFoundException("Tài khoản chưa đồng bộ");
        if (!active && db.queryForObject("SELECT COUNT(*) FROM user_roles ur JOIN roles r ON r.id=ur.role_id WHERE ur.user_uid=? AND r.code='ADMIN'",Integer.class,uid)>0
                && db.queryForObject("SELECT COUNT(*) FROM user_roles ur JOIN roles r ON r.id=ur.role_id JOIN users u ON u.uid=ur.user_uid WHERE r.code='ADMIN' AND u.status='ACTIVE'",Integer.class)<=1)
            fail("LAST_ADMIN","Không thể khóa quản trị viên cuối cùng");
        String status=active ? "ACTIVE" : "DISABLED";
        db.update("UPDATE users SET status=?,updated_at=CURRENT_TIMESTAMP WHERE uid=?",status,uid);
        if (!active) db.update("UPDATE device_tokens SET active=FALSE WHERE user_uid=?",uid);
        events.audit("CHANGE_USER_STATUS","USER",uid,users.get(0).get("status").toString(),status);
    }
    private void fail(String code,String message) { throw new BusinessRuleException(code,message); }
}
