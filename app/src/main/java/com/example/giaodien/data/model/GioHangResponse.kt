package com.example.giaodien.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GioHangResponse(
    val id: Long,
    val idDat: Long,
    val idThucDon: Long,
    val tenMon: String,
    val soLuong: Int,
    val giaMon: Double,
    val thanhTien: Double
)
