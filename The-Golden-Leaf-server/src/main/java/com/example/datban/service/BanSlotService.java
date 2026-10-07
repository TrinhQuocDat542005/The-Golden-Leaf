package com.example.datban.service;

import com.example.datban.model.BanSlot;
import com.example.datban.repository.BanSlotRepository;
import com.example.datban.exception.BusinessRuleException;
import com.example.datban.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khung giờ đã chọn"));
    }

    // Đặt bàn theo số lượng khách
    @Transactional
    public BanSlot datBan(LocalDate ngay, String khungGio, int soLuongKhach) {
        BanSlot slot = getSlot(ngay, khungGio);

        int soBanCan = (int) Math.ceil(soLuongKhach / 8.0);

        if (slot.getSoBanConLai() < soBanCan) {
            throw new BusinessRuleException("NOT_ENOUGH_TABLES", "Không đủ bàn trống cho số lượng khách đã chọn");
        }

        slot.setSoBanConLai(slot.getSoBanConLai() - soBanCan);
        return repo.save(slot);
    }

    // Trả bàn
    @Transactional
    public BanSlot traBan(LocalDate ngay, String khungGio, int soLuongKhach) {
        BanSlot slot = getSlot(ngay, khungGio);
        int soBanCan = (int) Math.ceil(soLuongKhach / 8.0);
        slot.setSoBanConLai(Math.min(slot.getSoBanBanDau(), slot.getSoBanConLai() + soBanCan));
        return repo.save(slot);
    }
}
