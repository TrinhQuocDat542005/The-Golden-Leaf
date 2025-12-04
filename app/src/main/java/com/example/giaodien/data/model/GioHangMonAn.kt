package com.example.giaodien.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GioHangMonAn(
    // Liên kết với DatBan.idDat (Server: id_dat)
    val idDat: Long,

    // Thông tin chi tiết món ăn (Server: id_thuc_don, ten_mon, so_luong, gia_mon)
    val idThucDon: Long,
    val tenMon: String,
    val soLuong: Int,
    val giaMon: Double
)