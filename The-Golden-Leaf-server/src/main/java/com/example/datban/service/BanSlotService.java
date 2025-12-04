package com.example.datban.service;

import com.example.datban.model.BanSlot;
import com.example.datban.repository.BanSlotRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class BanSlotService {

    private final BanSlotRepository repo;

    public BanSlotService(BanSlotRepository repo) {
        this.repo = repo;
    }

    // Lấy tất cả slot
    public List<BanSlot> getAllSlots() {
        return repo.findAll();
    }

    // Lấy slot theo ngày + khung
    public BanSlot getSlot(LocalDate ngay, String khungGio) {
        return repo.findByNgayAndKhungGio(ngay, khungGio)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy slot"));
    }

    // Đặt bàn theo số lượng khách
    public BanSlot datBan(LocalDate ngay, String khungGio, int soLuongKhach) {
        BanSlot slot = getSlot(ngay, khungGio);

        int soBanCan = (int) Math.ceil(soLuongKhach / 8.0);

        if (slot.getSoBanConLai() < soBanCan) {
            throw new RuntimeException("Không đủ bàn trống");
        }

        slot.setSoBanConLai(slot.getSoBanConLai() - soBanCan);
        return repo.save(slot);
    }

    // Trả bàn
    public BanSlot traBan(LocalDate ngay, String khungGio, int soLuongKhach) {
        BanSlot slot = getSlot(ngay, khungGio);
        int soBanCan = (int) Math.ceil(soLuongKhach / 8.0);
        slot.setSoBanConLai(slot.getSoBanConLai() + soBanCan);
        return repo.save(slot);
    }
}
