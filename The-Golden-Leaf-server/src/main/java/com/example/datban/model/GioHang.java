package com.example.datban.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "booking_items")
public class GioHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính tự tăng

    // --- KHÓA NGOẠI: LIÊN KẾT VỚI BẢNG DAT_BAN ---
    @Column(name = "booking_id", nullable = false)
    private Long idDat; // Liên kết với DatBan.idDat (id_ban)

    // --- THÔNG TIN KHÁCH HÀNG ---
    @Column(name = "customer_email", nullable = false)
    private String email; // Email khách hàng

    // --- THÔNG TIN CHI TIẾT MÓN ĂN ---
    @Column(name = "menu_item_id", nullable = false)
    private Long idThucDon; // ID của món ăn trong bảng ThucDon

    @Column(name = "item_name", nullable = false)
    private String tenMon; // Tên món (để hiển thị nhanh)

    @Column(name = "quantity", nullable = false)
    private Integer soLuong; // Số lượng món khách đặt

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal giaMon; // Giá món tại thời điểm đặt

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal thanhTien;

    // Constructor không tham số
    public GioHang() {}

    // Constructor đầy đủ tham số
    public GioHang(Long idDat, String email, Long idThucDon, String tenMon, Integer soLuong, BigDecimal giaMon) {
        this.idDat = idDat;
        this.email = email;
        this.idThucDon = idThucDon;
        this.tenMon = tenMon;
        this.soLuong = soLuong;
        this.giaMon = giaMon;
        this.thanhTien = giaMon.multiply(BigDecimal.valueOf(soLuong));
    }

    // Getter & Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getIdDat() { return idDat; }
    public void setIdDat(Long idDat) { this.idDat = idDat; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Long getIdThucDon() { return idThucDon; }
    public void setIdThucDon(Long idThucDon) { this.idThucDon = idThucDon; }

    public String getTenMon() { return tenMon; }
    public void setTenMon(String tenMon) { this.tenMon = tenMon; }

    public Integer getSoLuong() { return soLuong; }
    public void setSoLuong(Integer soLuong) { this.soLuong = soLuong; }

    public BigDecimal getGiaMon() { return giaMon; }
    public void setGiaMon(BigDecimal giaMon) { this.giaMon = giaMon; }

    public BigDecimal getThanhTien() { return thanhTien; }
    public void setThanhTien(BigDecimal thanhTien) { this.thanhTien = thanhTien; }

    @Override
    public String toString() {
        return "GioHang{" +
                "id=" + id +
                ", idDat=" + idDat +
                ", email='" + email + '\'' +
                ", idThucDon=" + idThucDon +
                ", tenMon='" + tenMon + '\'' +
                ", soLuong=" + soLuong +
                ", giaMon=" + giaMon +
                '}';
    }
}
