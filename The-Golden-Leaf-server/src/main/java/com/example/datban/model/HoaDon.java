package com.example.datban.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hoa_don")
public class HoaDon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;   // ID thứ tự (auto increment)

    @Column(name = "id_dat", unique = true, nullable = false)
    private Long idDat;  // ID đặt bàn (duy nhất)

    @Column(name = "tien_ban", nullable = false)
    private Double tienBan;

    @Column(name = "tien_an", nullable = false)
    private Double tienAn;

    @Column(name = "tong_tien", nullable = false)
    private Double tongTien;

    @Column(name = "ngay_gio_thanh_toan", nullable = false)
    private LocalDateTime ngayGioThanhToan;

    // TỰ ĐỘNG GÁN NGÀY GIỜ THANH TOÁN LÚC TẠO RECORD
    @PrePersist
    protected void onCreate() {
        this.ngayGioThanhToan = LocalDateTime.now();
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

    public Double getTienBan() {
        return tienBan;
    }

    public void setTienBan(Double tienBan) {
        this.tienBan = tienBan;
    }

    public Double getTienAn() {
        return tienAn;
    }

    public void setTienAn(Double tienAn) {
        this.tienAn = tienAn;
    }

    public Double getTongTien() {
        return tongTien;
    }

    public void setTongTien(Double tongTien) {
        this.tongTien = tongTien;
    }

    public LocalDateTime getNgayGioThanhToan() {
        return ngayGioThanhToan;
    }
}
