package com.example.datban.model;

import java.time.LocalDate;
import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name = "bookings")
public class DatBan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long idDat;

    @Column(name = "customer_email", nullable = false)
    private String email;

    @Column(name = "user_uid")
    private String userUid;
    public String getUserUid() { return userUid; }
    public void setUserUid(String uid) { this.userUid = uid; }

    @Column(name = "customer_name", nullable = false)
    private String ten;

    @Column(name = "booking_date", nullable = false)
    private LocalDate ngay;

    @Column(name = "slot_label", nullable = false)
    private String khungGio;

    @Column(name = "guest_count", nullable = false)
    private Integer soLuong;

    @Column(name = "note")
    private String ghiChu;

    @Column(name = "preferred_area_name", nullable = false)
    private String viTriBan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.HOLDING;

    @Column(name = "idempotency_key", unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "reserved_tables", nullable = false)
    private int reservedTables;

    @Column(name = "hold_expires_at")
    private Instant holdExpiresAt;

    @Version
    private long version;

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String key) { this.idempotencyKey = key; }
    public int getReservedTables() { return reservedTables; }
    public void setReservedTables(int count) { this.reservedTables = count; }
    public Instant getHoldExpiresAt() { return holdExpiresAt; }
    public void setHoldExpiresAt(Instant expiry) { this.holdExpiresAt = expiry; }

    // Constructor không tham số
    public DatBan() {}

    // Constructor đầy đủ tham số
   public DatBan(Long idDat, String email, String ten, LocalDate ngay,
              String khungGio, Integer soLuong, String ghiChu, String viTriBan) {
    this.idDat = idDat;
    this.email = email;
    this.ten = ten;
    this.ngay = ngay;
    this.khungGio = khungGio;
    this.soLuong = soLuong;
    this.ghiChu = ghiChu;
    this.viTriBan = viTriBan;
}

    // Getter & Setter
    public Long getIdDat() { return idDat; }
    public void setIdDat(Long idDat) { this.idDat = idDat; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTen() { return ten; }
    public void setTen(String ten) { this.ten = ten; }

    public LocalDate getNgay() { return ngay; }
    public void setNgay(LocalDate ngay) { this.ngay = ngay; }

    public String getKhungGio() { return khungGio; }
    public void setKhungGio(String khungGio) { this.khungGio = khungGio; }

    public Integer getSoLuong() { return soLuong; }
    public void setSoLuong(Integer soLuong) { this.soLuong = soLuong; }

    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }

    public String getViTriBan() { return viTriBan; }
public void setViTriBan(String viTriBan) { this.viTriBan = viTriBan; }


    @Override
    public String toString() {
        return "DatBan{" +
                "idDat=" + idDat +
                ", email='" + email + '\'' +
                ", ten='" + ten + '\'' +
                ", ngay=" + ngay +
                ", khungGio='" + khungGio + '\'' +
                ", soLuong=" + soLuong +
                ", ghiChu='" + ghiChu + '\'' +
                '}';
    }
}
