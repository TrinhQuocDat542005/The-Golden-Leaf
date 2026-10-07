package com.example.datban.config;

import com.example.datban.model.BanSlot;
import com.example.datban.repository.BanSlotRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
public class BanSlotInitializer {
    private static final List<String> LABELS = List.of("07:00-11:00", "11:00-15:00", "15:00-19:00", "19:00-23:00");
    private final BanSlotRepository repo;
    private final Clock clock;
    private final ZoneId zone;

    public BanSlotInitializer(BanSlotRepository repo, Clock clock,
            @Value("${app.booking.zone:Asia/Ho_Chi_Minh}") String zone) {
        this.repo = repo;
        this.clock = clock;
        this.zone = ZoneId.of(zone);
    }

    @Bean
    CommandLineRunner initBanSlot() { return args -> refreshSlots(); }

    @Scheduled(cron = "0 5 0 * * *", zone = "${app.booking.zone:Asia/Ho_Chi_Minh}")
    public void refreshSlots() {
        LocalDate today = LocalDate.now(clock.withZone(zone));
        // Retain historical inventory for expiry/cancellation; never reset remaining capacity.
        for (int i = 0; i < 7; i++) {
            LocalDate date = today.plusDays(i);
            for (String label : LABELS) {
                if (repo.findByNgayAndKhungGio(date, label).isEmpty()) {
                    BanSlot slot = new BanSlot();
                    slot.setNgay(date);
                    slot.setKhungGio(label);
                    slot.setSoBanBanDau(30);
                    slot.setSoBanConLai(30);
                    try {
                        repo.saveAndFlush(slot);
                    } catch (DataIntegrityViolationException conflict) {
                        if (repo.findByNgayAndKhungGio(date, label).isEmpty()) { throw conflict; }
                    }
                }
            }
        }
    }
}
