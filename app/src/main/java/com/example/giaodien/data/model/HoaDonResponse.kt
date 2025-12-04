package com.example.giaodien.data.model

import kotlinx.serialization.Serializable

@Serializable
data class HoaDonResponse(
    val id: Long,
    val idDat: Long,
    val tienBan: Double,
    val tienAn: Double,
    val tongTien: Double,
    val ngayGioThanhToan: String
)

