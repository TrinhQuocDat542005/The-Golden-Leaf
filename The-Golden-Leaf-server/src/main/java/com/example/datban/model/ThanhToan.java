package com.example.datban.model;

import jakarta.persistence.*;

@Entity
@Table(name = "thanh_toan")
public class ThanhToan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idThanhToan; // Khóa chính tự tăng

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "ngay_gio", nullable = false)
    private String ngayGio;

    @Column(name = "trang_thai", nullable = false)
    private String status;

    // Constructor không tham số
    public ThanhToan() {}

    // Constructor đầy đủ tham số
    public ThanhToan(Long idThanhToan, String email, String ngayGio, String status) {
        this.idThanhToan = idThanhToan;
        this.email = email;
        this.ngayGio = ngayGio;
        this.status = status;
    }

    // Getter & Setter
    public Long getIdThanhToan() { return idThanhToan; }
    public void setIdThanhToan(Long idThanhToan) { this.idThanhToan = idThanhToan; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNgayGio() { return ngayGio; }
    public void setNgayGio(String ngayGio) { this.ngayGio = ngayGio; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "ThanhToan{" +
                "idThanhToan=" + idThanhToan +
                ", email='" + email + '\'' +
                ", ngayGio='" + ngayGio + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
