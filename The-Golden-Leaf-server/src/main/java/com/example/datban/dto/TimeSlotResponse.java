package com.example.datban.dto;

import com.example.datban.model.BanSlot;
import java.time.LocalDate;

public record TimeSlotResponse(
        Long id,
        LocalDate ngay,
        String khungGio,
        int soBanBanDau,
        int soBanConLai,
        int soGheConLai
) {
    public static TimeSlotResponse from(BanSlot slot) {
        return new TimeSlotResponse(
                slot.getId(), slot.getNgay(), slot.getKhungGio(), slot.getSoBanBanDau(),
                slot.getSoBanConLai(), slot.getSoGheConLai());
    }
}
