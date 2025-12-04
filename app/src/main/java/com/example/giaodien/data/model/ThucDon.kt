package com.example.giaodien.data.model
import kotlinx.serialization.Serializable
@Serializable
data class ThucDon(
    val idThucDon: Long = 0,
    val tenMon: String = "",
    val gia: Double = 0.0,
    val moTa: String = "",
    val anh: String = "",
    val nhom: String = ""
)
