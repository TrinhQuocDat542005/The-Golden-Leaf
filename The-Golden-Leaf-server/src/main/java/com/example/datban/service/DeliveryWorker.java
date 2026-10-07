package com.example.datban.service;

import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.*;

/** Durable at-least-once delivery, bounded retries and expiring leases; no network call in a DB transaction. */
@Service
public class DeliveryWorker {
    private final JdbcTemplate db;
    private final PushGateway gateway;
    private final TransactionTemplate tx;
    private final Clock clock;
    public DeliveryWorker(JdbcTemplate db,PushGateway gateway,PlatformTransactionManager manager,Clock clock) {
        this.db=db;this.gateway=gateway;this.tx=new TransactionTemplate(manager);this.clock=clock;
    }
    public void runBatch() {
        for (int i=0;i<20;i++) { if (!deliverOne()) break; }
    }
    public boolean deliverOne() {
        String lease=UUID.randomUUID().toString();
        Long id=tx.execute(s->{
            var ids=db.queryForList("SELECT id FROM notification_deliveries WHERE (status='PENDING' AND next_attempt_at<=?) OR (status='SENDING' AND lease_until<=?) ORDER BY id LIMIT 1 FOR UPDATE",Long.class,now(),now());
            if (ids.isEmpty()) return null;
            db.update("UPDATE notification_deliveries SET status='SENDING',attempts=attempts+1,lease_key=?,lease_until=? WHERE id=?",lease,Timestamp.from(clock.instant().plusSeconds(120)),ids.get(0));
            return ids.get(0);
        });
        if (id==null) return false;
        var rows=db.queryForList("SELECT d.token,d.id AS device_id,n.id AS notification_id,n.user_uid,n.title,n.message,n.booking_id,n.type,o.attempts FROM notification_deliveries o JOIN notifications n ON n.id=o.notification_id JOIN device_tokens d ON d.id=o.device_id AND d.active=TRUE AND d.user_uid=n.user_uid WHERE o.id=? AND o.lease_key=?",id,lease);
        if (rows.isEmpty()) { finish(id,lease,"CANCELLED",null,0,false,null);return true; }
        var row=rows.get(0);
        if ("PAYMENT_PENDING".equals(row.get("type")) && db.queryForObject("SELECT COUNT(*) FROM payments WHERE booking_id=? AND status='PENDING'",Integer.class,row.get("booking_id"))==0) {
            finish(id,lease,"CANCELLED","OBSOLETE_PAYMENT_REQUEST",0,false,null); return true;
        }
        if (((Number)row.get("attempts")).intValue()>5) {
            finish(id,lease,"FAILED","LEASE_RETRY_LIMIT",0,false,null); return true;
        }
        Map<String,String> data=Map.of("notificationId",row.get("notification_id").toString(),"userUid",row.get("user_uid").toString(),
                "title",row.get("title").toString(),"message",row.get("message").toString(),"bookingId",String.valueOf(row.get("booking_id")));
        try {
            gateway.send(row.get("token").toString(),data);
            finish(id,lease,"DELIVERED",null,0,false,null);
        } catch (PushGateway.Failure ex) {
            int attempt=((Number)row.get("attempts")).intValue();
            finish(id,lease,ex.retryable && attempt<5 ? "PENDING" : "FAILED",ex.code,Math.min(3600,60*(1<<Math.min(attempt,5))),ex.invalidToken,row.get("device_id"));
        } catch (RuntimeException ex) {
            int attempt=((Number)row.get("attempts")).intValue();
            finish(id,lease,attempt<5 ? "PENDING" : "FAILED","DELIVERY_ERROR",300,false,null);
        }
        return true;
    }
    private void finish(Long id,String lease,String status,String error,int delay,boolean invalid,Object deviceId) {
        tx.executeWithoutResult(s->{
            int changed=db.update("UPDATE notification_deliveries SET status=?,last_error=?,next_attempt_at=?,lease_until=NULL,lease_key=NULL WHERE id=? AND lease_key=?",status,error,Timestamp.from(clock.instant().plusSeconds(delay)),id,lease);
            if (changed==1 && invalid) db.update("UPDATE device_tokens SET active=FALSE WHERE id=?",deviceId);
        });
    }
    private Timestamp now() { return Timestamp.from(clock.instant()); }
}
