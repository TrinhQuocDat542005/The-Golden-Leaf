package com.example.giaodien.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Payment(
    val id: Long,
    val bookingId: Long,
    val amount: Double,
    val currency: String,
    val status: String,
    val reference: String,
    val bankName: String,
    val accountNumber: String,
    val accountName: String,
    val paidAt: String? = null
)

@Serializable
data class DeviceTokenRequest(val token: String)

@Serializable
data class UnreadNotificationCount(val count: Int)

@Serializable
data class PaymentQuote(val idDat: Long, val tienBan: Double, val tienAn: Double, val tongTien: Double, val currency: String)
