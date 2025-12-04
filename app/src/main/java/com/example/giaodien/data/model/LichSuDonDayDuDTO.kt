package com.example.giaodien.data.model

import com.example.giaodien.model.GioHang
import kotlinx.serialization.Serializable

/**
 * DTO này phải khớp với LichSuDonDayDuDTO.java trả về từ server.
 * Chú ý: Java sử dụng LocalDateTime/LocalDate, Kotlin nên nhận String và xử lý sau.
 */
@Serializable
data class LichSuDonDayDuDTO(
    val id: Long? = null,
    val idDat: Long,
    val soBan: String? = null,
    // Server trả về LocalDateTime, client nhận String
    val thoiGianPhanBan: String? = null,
    val email: String,
    val ten: String,
    val ngay: String, // LocalDate -> String
    val khungGio: String,
    val soLuong: Int,
    val ghiChu: String? = null,
    val viTriBan: String,
    val tienBan: Double? = null,
    val tienAn: Double? = null,
    val tongTien: Double? = null,
    // Server trả về LocalDateTime, client nhận String
    val ngayGioThanhToan: String? = null,
    val danhSachMon: List<GioHang> = emptyList()
)