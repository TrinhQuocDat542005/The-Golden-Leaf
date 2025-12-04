package com.example.datban.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "ban_slot")
public class BanSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate ngay;

    private String khungGio;

    private int soBanBanDau;

    private int soBanConLai;

    // Getter / Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getNgay() { return ngay; }
    public void setNgay(LocalDate ngay) { this.ngay = ngay; }

    public String getKhungGio() { return khungGio; }
    public void setKhungGio(String khungGio) { this.khungGio = khungGio; }

    public int getSoBanBanDau() { return soBanBanDau; }
    public void setSoBanBanDau(int soBanBanDau) { this.soBanBanDau = soBanBanDau; }

    public int getSoBanConLai() { return soBanConLai; }
    public void setSoBanConLai(int soBanConLai) { this.soBanConLai = soBanConLai; }

    // Số ghế còn lại (tính động)
    @Transient
    public int getSoGheConLai() {
        return this.soBanConLai * 8;
    }
}
