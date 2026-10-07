package com.example.datban.service;

import com.example.datban.dto.HoaDonRequest;
import com.example.datban.model.HoaDon;
import com.example.datban.exception.BusinessRuleException;
import com.example.datban.repository.HoaDonRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import com.example.datban.repository.GioHangRepository;
import com.example.datban.model.BookingStatus;
import org.springframework.beans.factory.annotation.Value;

@Service
public class HoaDonService {

    private final HoaDonRepository repo;
    private final BookingLifecycleService lifecycle;
    private final GioHangRepository items;
    private final BigDecimal tableFee;

    public HoaDonService(HoaDonRepository repo, BookingLifecycleService lifecycle, GioHangRepository items,
            @Value("${app.booking.table-fee:200000.00}") BigDecimal tableFee) {
        this.repo = repo;
        this.lifecycle = lifecycle;
        this.items = items;
        if (tableFee.signum() < 0 || tableFee.scale() > 2 || tableFee.precision() - tableFee.scale() > 10) {
            throw new IllegalArgumentException("Invalid booking table fee");
        }
        this.tableFee = tableFee;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public HoaDon saveHoaDon(HoaDonRequest req, String actorEmail) {
        var booking = lifecycle.lock(req.idDat(), actorEmail);
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.ASSIGNED) {
            throw new BusinessRuleException("INVALID_BOOKING_STATE", "Cần xác nhận đặt bàn trước khi lập hóa đơn");
        }
        var existing = repo.findByIdDat(req.idDat());
        if (existing.isPresent()) {
            HoaDon invoice = existing.get();
            validateTotals(req, invoice.getTienBan(), invoice.getTienAn());
            return invoice;
        }
        BigDecimal foodTotal = items.findByIdDat(req.idDat()).stream()
                .map(item -> item.getThanhTien()).reduce(BigDecimal.ZERO, BigDecimal::add);
        validateTotals(req, tableFee, foodTotal);

        HoaDon hd = new HoaDon();
        hd.setIdDat(req.idDat());
        hd.setTienBan(tableFee);
        hd.setTienAn(foodTotal);
        hd.setTongTien(tableFee.add(foodTotal));

        return repo.save(hd);
    }

    private void validateTotals(HoaDonRequest req, BigDecimal fee, BigDecimal foodTotal) {
        if (req.tienBan().compareTo(fee) != 0 || req.tienAn().compareTo(foodTotal) != 0
                || req.tongTien().compareTo(fee.add(foodTotal)) != 0) {
            throw new BusinessRuleException("INVALID_INVOICE_TOTAL", "Tổng tiền không khớp dữ liệu trên máy chủ");
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public HoaDon ensureInvoice(Long id, String actorEmail) {
        var booking = lifecycle.lock(id, actorEmail);
        var existing = repo.findByIdDat(id);
        if (existing.isPresent()) return existing.get();
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.ASSIGNED) {
            throw new BusinessRuleException("INVALID_BOOKING_STATE", "Cần xác nhận đặt bàn trước khi lập hóa đơn");
        }
        BigDecimal food = items.findByIdDat(id).stream().map(i -> i.getThanhTien()).reduce(BigDecimal.ZERO, BigDecimal::add);
        return saveHoaDon(new HoaDonRequest(id, tableFee, food, tableFee.add(food)), actorEmail);
    }
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public com.example.datban.dto.PaymentQuote preview(Long id,String actorEmail) {
        var booking=lifecycle.lock(id,actorEmail);
        var existing=repo.findByIdDat(id);
        if(existing.isPresent()) {
            var invoice=existing.get();return new com.example.datban.dto.PaymentQuote(id,invoice.getTienBan(),invoice.getTienAn(),invoice.getTongTien(),invoice.getCurrency());
        }
        if(booking.getStatus()!=BookingStatus.CONFIRMED && booking.getStatus()!=BookingStatus.ASSIGNED)
            throw new BusinessRuleException("INVALID_BOOKING_STATE","Cần xác nhận đặt bàn trước khi xem tổng tiền");
        BigDecimal food=items.findByIdDat(id).stream().map(i->i.getThanhTien()).reduce(BigDecimal.ZERO,BigDecimal::add);
        return new com.example.datban.dto.PaymentQuote(id,tableFee,food,tableFee.add(food),"VND");
    }
}
