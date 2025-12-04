package com.example.giaodien.data.model

import kotlinx.serialization.Serializable

@Serializable
data class HoaDonRequest(
    val idDat: Long,
    val tienBan: Double,
    val tienAn: Double,
    val tongTien: Double
)
