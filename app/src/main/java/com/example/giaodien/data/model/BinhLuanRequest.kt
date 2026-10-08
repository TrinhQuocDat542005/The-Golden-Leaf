package com.example.giaodien.data.model

import kotlinx.serialization.Serializable

@Serializable
data class BinhLuanRequest(
    val thucDonId: Long,
    val noiDung: String,
    val rating: Int
)
