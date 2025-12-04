package com.example.datban.model;

import jakarta.persistence.*;

@Entity
@Table(name = "thuc_don")
public class ThucDon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idThucDon;

    private String tenMon;
    private double gia;
    private String moTa;
    private String anh;

    @Enumerated(EnumType.STRING)
    private NhomMon nhom;  // thêm cột mới

    // Getters và Setters
    public Long getIdThucDon() { return idThucDon; }
    public void setIdThucDon(Long idThucDon) { this.idThucDon = idThucDon; }

    public String getTenMon() { return tenMon; }
    public void setTenMon(String tenMon) { this.tenMon = tenMon; }

    public double getGia() { return gia; }
    public void setGia(double gia) { this.gia = gia; }

    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }

    public String getAnh() { return anh; }
    public void setAnh(String anh) { this.anh = anh; }

    public NhomMon getNhom() { return nhom; }
    public void setNhom(NhomMon nhom) { this.nhom = nhom; }
}
