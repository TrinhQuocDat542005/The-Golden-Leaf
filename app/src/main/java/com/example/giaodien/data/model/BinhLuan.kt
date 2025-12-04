package com.example.giaodien.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ThucDonRef(
    val idThucDon: Long,
    val tenMon: String? = null
)

@Serializable
data class BinhLuan(
    val id: Long,
    val thucDon: ThucDonRef, // đổi từ Long sang object
    val userEmail: String,
    val noiDung: String,
    val createdAt: String
)
