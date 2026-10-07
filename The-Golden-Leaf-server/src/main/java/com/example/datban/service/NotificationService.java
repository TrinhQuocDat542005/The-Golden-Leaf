package com.example.datban.service;

import com.example.datban.security.RestaurantPrincipal;
import com.example.datban.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class NotificationService {
    private final JdbcTemplate db;
    public NotificationService(JdbcTemplate db) { this.db=db; }
    public List<Map<String,Object>> list(String uid) {
        return db.query("SELECT * FROM notifications WHERE user_uid=? ORDER BY id DESC LIMIT 100",(rs,n)->{
            Map<String,Object> result=new LinkedHashMap<>(); result.put("id",rs.getLong("id"));
            result.put("bookingId",rs.getObject("booking_id")); result.put("type",rs.getString("type"));
            result.put("title",rs.getString("title"));result.put("message",rs.getString("message"));
            result.put("status",rs.getString("status"));result.put("readFlag",rs.getBoolean("read_flag"));
            result.put("createdAt",rs.getTimestamp("created_at").toInstant().toString()); return result;
        },uid);
    }
    public void read(String uid,Long id) {
        if (db.update("UPDATE notifications SET read_flag=TRUE,read_at=CURRENT_TIMESTAMP WHERE id=? AND user_uid=?",id,uid)!=1)
            throw new ResourceNotFoundException("Không tìm thấy thông báo");
    }
    public int unread(String uid) { return db.queryForObject("SELECT COUNT(*) FROM notifications WHERE user_uid=? AND read_flag=FALSE",Integer.class,uid); }
    @Transactional
    public void register(String uid,String token) {
        // Serialize registrations to avoid duplicate provisioning on MySQL and H2.
        db.queryForList("SELECT id FROM roles WHERE code='CUSTOMER' FOR UPDATE");
        var rows=db.queryForList("SELECT id,user_uid FROM device_tokens WHERE token=? FOR UPDATE",token);
        if (rows.isEmpty()) db.update("INSERT INTO device_tokens(user_uid,token) VALUES(?,?)",uid,token);
        else {
            Long device=((Number)rows.get(0).get("id")).longValue();
            // A shared device must never receive queued notifications for the previous account.
            if (!uid.equals(rows.get(0).get("user_uid"))) db.update("DELETE FROM notification_deliveries WHERE device_id=?",device);
            db.update("UPDATE device_tokens SET user_uid=?,active=TRUE,updated_at=CURRENT_TIMESTAMP WHERE id=?",uid,device);
        }
    }
    public void unregister(String uid,String token) {
        db.update("UPDATE device_tokens SET active=FALSE,updated_at=CURRENT_TIMESTAMP WHERE user_uid=? AND token=?",uid,token);
    }
}
