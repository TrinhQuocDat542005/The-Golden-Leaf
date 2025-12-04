package com.example.giaodien.data.model

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class BanSlot(
    val id: Long = 0L,
    val ngay: String,
    val khungGio: String,
    val soBan: Int,
    val daDat: Boolean
)


