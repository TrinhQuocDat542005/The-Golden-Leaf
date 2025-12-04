package com.example.datban.config;

import com.example.datban.model.BanSlot;
import com.example.datban.repository.BanSlotRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.List;

@Configuration
public class BanSlotInitializer {

    private static final List<String> KHUNG_GIO = List.of(
            "07:00-11:00",
            "11:00-15:00",
            "15:00-19:00",
            "19:00-23:00"
    );

    private static final int SO_BAN_MOI_KHUNG = 30;

    @Bean
    CommandLineRunner initBanSlot(BanSlotRepository repo) {
        return args -> {
            LocalDate today = LocalDate.now();

            // Lấy ngày lớn nhất trong DB
            LocalDate maxNgay = repo.findAll().stream()
                    .map(BanSlot::getNgay)
                    .max(LocalDate::compareTo)
                    .orElse(today.minusDays(1));

            // Xóa các ngày đã qua (tuỳ bạn)
            repo.findAll().stream()
                .filter(slot -> slot.getNgay().isBefore(today))
                .forEach(repo::delete);

            // Thêm các ngày mới để đảm bảo luôn 7 ngày
            for (int i = 0; i < 7; i++) {
                LocalDate ngay = today.plusDays(i);
                for (String khung : KHUNG_GIO) {
                    boolean exists = repo.findByNgayAndKhungGio(ngay, khung).isPresent();
                    if (!exists) {
                        BanSlot slot = new BanSlot();
                        slot.setNgay(ngay);
                        slot.setKhungGio(khung);
                        slot.setSoBanBanDau(SO_BAN_MOI_KHUNG);
                        slot.setSoBanConLai(SO_BAN_MOI_KHUNG);
                        repo.save(slot);
                    }
                }
            }

            System.out.println("✅ Đã cập nhật dữ liệu bàn tự động, đảm bảo luôn 7 ngày tiếp theo.");
        };
    }
}
