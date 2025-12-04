package com.example.giaodien.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    val id: Long,
    val bookingId: Long? = null,
    val userId: Long? = null,
    val userEmail: String? = null,
    val type: String? = null,
    val title: String? = null,
    val message: String,
    val status: String? = null,
    val readFlag: Boolean = false,
    val createdAt: String? = null

)
