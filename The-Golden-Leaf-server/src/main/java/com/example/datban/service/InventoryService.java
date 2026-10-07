package com.example.datban.service;

import com.example.datban.repository.BanSlotRepository;
import com.example.datban.exception.BusinessRuleException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import java.time.*;

@Service
public class InventoryService {
    private final JdbcTemplate db;
    private final BanSlotRepository slots;
    private final Clock clock;
    private final ZoneId zone;
    private final BookingEvents events;
    public InventoryService(JdbcTemplate db,BanSlotRepository slots,Clock clock,BookingEvents events,
            @Value("${app.booking.zone:Asia/Ho_Chi_Minh}") String zone) {
        this.db=db;this.slots=slots;this.clock=clock;this.events=events;this.zone=ZoneId.of(zone);
    }
    public int physicalCount() {
        // One reservation unit represents eight seats. Smaller tables cannot supply such a unit.
        return db.queryForObject("SELECT COUNT(*) FROM restaurant_tables t JOIN restaurant_areas a ON a.id=t.area_id WHERE t.active=TRUE AND a.active=TRUE AND t.status='AVAILABLE' AND t.capacity>=8",Integer.class);
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public int reconcile() {
        int capacity=physicalCount();
        if(capacity==0)throw new BusinessRuleException("INVENTORY_NOT_CONFIGURED","Cần khai báo bàn thực tế có tối thiểu 8 ghế");
        LocalDate today=LocalDate.now(clock.withZone(zone));
        // Scalar identities avoid a stale entity snapshot before acquiring each inventory lock.
        var identities=db.queryForList("SELECT booking_date,slot_label FROM time_slots WHERE booking_date BETWEEN ? AND ? ORDER BY booking_date,slot_label",today,today.plusDays(6));
        for(var identity:identities){
            LocalDate date=((java.sql.Date)identity.get("booking_date")).toLocalDate();
            var slot=slots.lockSlot(date,identity.get("slot_label").toString()).orElseThrow();
            int reserved=slot.getSoBanBanDau()-slot.getSoBanConLai();
            if(reserved>capacity)throw new BusinessRuleException("CAPACITY_CONFLICT","Số bàn thực tế ít hơn số bàn đang giữ. Cần xử lý các đơn trước khi giảm sức chứa.");
            slot.setSoBanBanDau(capacity);slot.setSoBanConLai(capacity-reserved);
        }
        events.audit("RECONCILE_INVENTORY","INVENTORY",today,null,"physical_tables="+capacity);
        return capacity;
    }
}
