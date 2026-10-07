package com.example.datban.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "menu_items")
public class ThucDon {
    @Column(nullable = false)
    private boolean active = true;

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long idThucDon;

    @Column(name = "name", nullable = false)
    private String tenMon;
    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal gia;
    @Column(name = "description")
    private String moTa;
    @Column(name = "image_url")
    private String anh;

    @Enumerated(EnumType.STRING)
    @Column(name = "legacy_group")
    private NhomMon nhom;  // thêm cột mới

    // Getters và Setters
    public Long getIdThucDon() { return idThucDon; }
    public void setIdThucDon(Long idThucDon) { this.idThucDon = idThucDon; }

    public String getTenMon() { return tenMon; }
    public void setTenMon(String tenMon) { this.tenMon = tenMon; }

    public BigDecimal getGia() { return gia; }
    public void setGia(BigDecimal gia) { this.gia = gia; }

    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }

    public String getAnh() { return anh; }
    public void setAnh(String anh) { this.anh = anh; }

    public NhomMon getNhom() { return nhom; }
    public void setNhom(NhomMon nhom) { this.nhom = nhom; }
}
