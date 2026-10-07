package com.example.giaodien.data.model

import kotlinx.serialization.Serializable
@Serializable
data class BanSlot(
    val id: Long = 0L,
    val ngay: String,
    val khungGio: String,
    val soBanBanDau: Int,
    val soBanConLai: Int,
    val soGheConLai: Int
)


