package com.example.datban.service;

import com.example.datban.model.DatBan;
import com.example.datban.security.RestaurantPrincipal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.time.Clock;
import java.sql.Timestamp;

/** Called in the booking transaction: a rollback also removes the notification/audit. */
@Service
public class BookingEvents {
    private final JdbcTemplate db;
    private final Clock clock;
    public BookingEvents(JdbcTemplate db, Clock clock) { this.db = db; this.clock = clock; }
    public void emit(DatBan booking, String type, String message) {
        if (booking.getUserUid() == null) return;
        String key = booking.getIdDat() + ":" + type;
        if (db.queryForObject("SELECT COUNT(*) FROM notifications WHERE event_key = ?", Integer.class, key) > 0) return;
        db.update("INSERT INTO notifications(user_uid,booking_id,type,title,message,event_key,created_at) VALUES(?,?,?,?,?,?,?)",
                booking.getUserUid(), booking.getIdDat(), type, "The Golden Leaf", message, key, Timestamp.from(clock.instant()));
        db.update("INSERT INTO notification_deliveries(notification_id,device_id,next_attempt_at) SELECT n.id,d.id,? FROM notifications n JOIN device_tokens d ON d.user_uid=n.user_uid AND d.active=TRUE WHERE n.event_key=?", Timestamp.from(clock.instant()), key);
    }
    public void audit(String action, String entity, Object id, String before, String after) {
        var p = RestaurantPrincipal.current();
        db.update("INSERT INTO audit_logs(actor_uid,action,entity_type,entity_id,old_value,new_value) VALUES(?,?,?,?,?,?)",
                p == null ? null : p.uid(), action, entity, String.valueOf(id), before, after);
    }
    public void cancelled(DatBan booking) {
        db.update("UPDATE payments SET status=CASE WHEN status='PAID' THEN 'REFUND_REQUIRED' ELSE 'CANCELLED' END WHERE booking_id=? AND status IN ('PAID','PENDING')", booking.getIdDat());
        db.update("UPDATE booking_tables SET released_at=? WHERE booking_id=? AND released_at IS NULL", Timestamp.from(clock.instant()), booking.getIdDat());
        emit(booking, "CANCELLED", "Đơn #" + booking.getIdDat() + " đã hủy. Khoản đã thu sẽ được đối soát hoàn tiền.");
        audit("CANCEL", "BOOKING", booking.getIdDat(), null, booking.getStatus().name());
    }
}
