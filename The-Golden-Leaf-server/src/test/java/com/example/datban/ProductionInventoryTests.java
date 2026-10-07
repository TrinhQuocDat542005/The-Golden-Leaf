package com.example.datban;

import com.example.datban.service.*;
import com.example.datban.repository.BanSlotRepository;
import com.example.datban.model.DatBan;
import com.example.datban.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.*;
import java.time.LocalDate;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:physical_inventory;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "app.security.require-auth=true","app.booking.require-physical-inventory=true"})
@ActiveProfiles("test")
@Import(OperationsTests.Fakes.class)
class ProductionInventoryTests {
    @Autowired BookingLifecycleService lifecycle;
    @Autowired InventoryService inventory;
    @Autowired BanSlotRepository slots;
    @Autowired JdbcTemplate db;
    static final LocalDate DATE=LocalDate.of(2026,10,7);
    @BeforeEach void reset() {
        db.update("DELETE FROM audit_logs");db.update("DELETE FROM bookings");db.update("DELETE FROM restaurant_tables");
        db.update("UPDATE time_slots SET initial_table_count=30,remaining_table_count=30");
    }
    DatBan request(){return new DatBan(null,"guest@example.com","Guest",DATE,"11:00-15:00",8,"","Trong nhà");}
    void physicalTable(){db.update("INSERT INTO restaurant_tables(area_id,code,capacity) SELECT MIN(id),'A01',8 FROM restaurant_areas");}
    @Test void productionRejectsPhantomDefaultCapacity() {
        assertThatThrownBy(()->lifecycle.create(request(),UUID.randomUUID().toString(),"guest@example.com"))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("bàn thực tế");
    }
    @Test void productionRejectsCapacityLargerThanPhysicalInventory() {
        physicalTable();assertThatThrownBy(()->lifecycle.create(request(),UUID.randomUUID().toString(),"guest@example.com"))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("bàn thực tế");
    }
    @Test void reconciledPhysicalInventoryCanAcceptOnlyItsRealCapacity() {
        physicalTable();inventory.reconcile();lifecycle.create(request(),UUID.randomUUID().toString(),"guest@example.com");
        assertThat(slots.findByNgayAndKhungGio(DATE,"11:00-15:00").orElseThrow().getSoBanConLai()).isZero();
        assertThatThrownBy(()->lifecycle.create(request(),UUID.randomUUID().toString(),"guest@example.com"))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("Không đủ bàn");
    }
}
