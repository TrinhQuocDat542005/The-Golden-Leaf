package com.example.giaodien.model

import kotlinx.serialization.Serializable

@Serializable
data class DatBanFullDTO(
    val datBan: DatBan,
    val hoaDon: HoaDon? = null,
    val gioHangList: List<GioHang> = emptyList(),
    val banSlotList: List<BanSlot> = emptyList()
)

@Serializable
data class DatBan(
    val idDat: Long,
    val ten: String,
    val email: String,
    val viTriBan: String,
    val ngay: String,
    val khungGio: String,
    val soLuong: Int
)

@Serializable
data class HoaDon(
    val idDat: Long,
    val tienBan: Double,
    val tienAn: Double,
    val tongTien: Double
)

@Serializable
data class GioHang(
    val idDat: Long,
    val tenMon: String,
    val soLuong: Int
)

@Serializable
data class BanSlot(
    val soBan: Int,
    val daDat: Boolean,
    val ngay: String,
    val khungGio: String
)
