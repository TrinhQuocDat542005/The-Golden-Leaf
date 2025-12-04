// package com.example.datban.dto;

// import java.time.LocalDate;
// import java.time.LocalDateTime;
// import java.util.List;
// import com.example.datban.model.BanSlot;


// public class DatBanInfoDTO {

//     private Long idDat;
//     private String email;
//     private String ten;
//     private LocalDate ngay;
//     private String khungGio;
//     private Integer soLuong;
//     private String ghiChu;
//     private String viTriBan;

//     private Double tienBan;
//     private Double tienAn;
//     private Double tongTien;
//     private LocalDateTime ngayGioThanhToan;

//     private List<GioHangItem> gioHangItems;

//     // --- GioHangItem inner class ---
//     public static class GioHangItem {
//         private String tenMon;
//         private Integer soLuong;
//         private Double giaMon;

//         public GioHangItem() {}

//         public GioHangItem(String tenMon, Integer soLuong, Double giaMon) {
//             this.tenMon = tenMon;
//             this.soLuong = soLuong;
//             this.giaMon = giaMon;
//         }

//         public String getTenMon() { return tenMon; }
//         public void setTenMon(String tenMon) { this.tenMon = tenMon; }

//         public Integer getSoLuong() { return soLuong; }
//         public void setSoLuong(Integer soLuong) { this.soLuong = soLuong; }

//         public Double getGiaMon() { return giaMon; }
//         public void setGiaMon(Double giaMon) { this.giaMon = giaMon; }
//     }

//     // --- Getters & Setters cho DatBan ---
//     public Long getIdDat() { return idDat; }
//     public void setIdDat(Long idDat) { this.idDat = idDat; }

//     public String getEmail() { return email; }
//     public void setEmail(String email) { this.email = email; }

//     public String getTen() { return ten; }
//     public void setTen(String ten) { this.ten = ten; }

//     public LocalDate getNgay() { return ngay; }
//     public void setNgay(LocalDate ngay) { this.ngay = ngay; }

//     public String getKhungGio() { return khungGio; }
//     public void setKhungGio(String khungGio) { this.khungGio = khungGio; }

//     public Integer getSoLuong() { return soLuong; }
//     public void setSoLuong(Integer soLuong) { this.soLuong = soLuong; }

//     public String getGhiChu() { return ghiChu; }
//     public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }

//     public String getViTriBan() { return viTriBan; }
//     public void setViTriBan(String viTriBan) { this.viTriBan = viTriBan; }

//     // --- Getters & Setters cho HoaDon ---
//     public Double getTienBan() { return tienBan; }
//     public void setTienBan(Double tienBan) { this.tienBan = tienBan; }

//     public Double getTienAn() { return tienAn; }
//     public void setTienAn(Double tienAn) { this.tienAn = tienAn; }

//     public Double getTongTien() { return tongTien; }
//     public void setTongTien(Double tongTien) { this.tongTien = tongTien; }

//     public LocalDateTime getNgayGioThanhToan() { return ngayGioThanhToan; }
//     public void setNgayGioThanhToan(LocalDateTime ngayGioThanhToan) { this.ngayGioThanhToan = ngayGioThanhToan; }

//     // --- Getter & Setter cho danh sách GioHang ---
//     public List<GioHangItem> getGioHangItems() { return gioHangItems; }
//     public void setGioHangItems(List<GioHangItem> gioHangItems) { this.gioHangItems = gioHangItems; }
// }
