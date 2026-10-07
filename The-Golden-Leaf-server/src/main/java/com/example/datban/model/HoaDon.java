package com.example.datban.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "invoices")
public class HoaDon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;   // ID thứ tự (auto increment)

    @Column(name = "booking_id", unique = true, nullable = false)
    private Long idDat;  // ID đặt bàn (duy nhất)

    @Column(name = "table_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal tienBan;

    @Column(name = "food_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal tienAn;

    @Column(name = "grand_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal tongTien;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "VND";

    @Column(name = "created_at", nullable = false)
    private Instant ngayGioThanhToan;

    // TỰ ĐỘNG GÁN NGÀY GIỜ THANH TOÁN LÚC TẠO RECORD
    @PrePersist
    protected void onCreate() {
        this.ngayGioThanhToan = Instant.now();
    }

    // Getter & Setter
    public Long getId() {
        return id;
    }

    public Long getIdDat() {
        return idDat;
    }

    public void setIdDat(Long idDat) {
        this.idDat = idDat;
    }

    public BigDecimal getTienBan() {
        return tienBan;
    }

    public void setTienBan(BigDecimal tienBan) {
        this.tienBan = tienBan;
    }

    public BigDecimal getTienAn() {
        return tienAn;
    }

    public void setTienAn(BigDecimal tienAn) {
        this.tienAn = tienAn;
    }

    public BigDecimal getTongTien() {
        return tongTien;
    }

    public void setTongTien(BigDecimal tongTien) {
        this.tongTien = tongTien;
    }

    public String getCurrency() { return currency; }

    public void setCurrency(String currency) { this.currency = currency; }

    public Instant getNgayGioThanhToan() {
        return ngayGioThanhToan;
    }
}
