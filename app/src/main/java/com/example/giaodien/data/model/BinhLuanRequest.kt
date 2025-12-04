package com.example.giaodien.data.network.model

import kotlinx.serialization.Serializable

@Serializable
data class BinhLuanRequest(
    val thucDonId: Long,
    val noiDung: String
)
