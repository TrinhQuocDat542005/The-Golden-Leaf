package com.example.datban.service;

import com.example.datban.dto.HoaDonRequest;
import com.example.datban.model.HoaDon;
import com.example.datban.exception.BusinessRuleException;
import com.example.datban.exception.ResourceNotFoundException;
import com.example.datban.repository.DatBanRepository;
import com.example.datban.repository.HoaDonRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HoaDonService {

    private final HoaDonRepository repo;
    private final DatBanRepository bookingRepository;

    public HoaDonService(HoaDonRepository repo, DatBanRepository bookingRepository) {
        this.repo = repo;
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public HoaDon saveHoaDon(HoaDonRequest req) {

        if (!bookingRepository.existsById(req.idDat())) {
            throw new ResourceNotFoundException("Không tìm thấy lượt đặt bàn để lập hóa đơn");
        }
        BigDecimal expectedTotal = req.tienBan().add(req.tienAn());
        if (expectedTotal.compareTo(req.tongTien()) != 0) {
            throw new BusinessRuleException("INVALID_INVOICE_TOTAL", "Tổng tiền không khớp tiền bàn và tiền món ăn");
        }

        HoaDon hd = new HoaDon();
        hd.setIdDat(req.idDat());
        hd.setTienBan(req.tienBan());
        hd.setTienAn(req.tienAn());
        hd.setTongTien(req.tongTien());

        return repo.save(hd);
    }
}
