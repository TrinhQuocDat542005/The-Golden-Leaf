package com.example.datban.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
public class ThanhToan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long idThanhToan; // Khóa chính tự tăng

    @Column(name = "booking_id", nullable = false)
    private Long idDat;

    @Column(nullable = false)
    private String provider;

    @Column(name = "provider_reference")
    private String providerReference;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "VND";

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "paid_at")
    private Instant paidAt;

    // Constructor không tham số
    public ThanhToan() {}

    // Constructor đầy đủ tham số
    // Getter & Setter
    public Long getIdThanhToan() { return idThanhToan; }
    public void setIdThanhToan(Long idThanhToan) { this.idThanhToan = idThanhToan; }

    public Long getIdDat() { return idDat; }
    public void setIdDat(Long idDat) { this.idDat = idDat; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getProviderReference() { return providerReference; }
    public void setProviderReference(String providerReference) { this.providerReference = providerReference; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
}
