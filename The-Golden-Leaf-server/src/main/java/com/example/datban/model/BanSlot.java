package com.example.datban.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "time_slots")
public class BanSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_date", nullable = false)
    private LocalDate ngay;

    @Column(name = "slot_label", nullable = false)
    private String khungGio;

    @Column(name = "initial_table_count", nullable = false)
    private int soBanBanDau;

    @Column(name = "remaining_table_count", nullable = false)
    private int soBanConLai;

    @Version
    private long version;

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
