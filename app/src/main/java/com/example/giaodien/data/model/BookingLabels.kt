package com.example.giaodien.data.model

fun bookingStatusLabel(status: String): String = when (status) {
    "HOLDING" -> "Đang giữ chỗ"
    "CONFIRMED" -> "Đã xác nhận"
    "ASSIGNED" -> "Đã phân bàn"
    "SEATED" -> "Đang phục vụ"
    "COMPLETED" -> "Đã hoàn tất"
    "CANCELLED" -> "Đã hủy"
    "EXPIRED" -> "Hết thời gian giữ chỗ"
    else -> "Đang cập nhật"
}

fun paymentStatusLabel(status: String?): String = when (status) {
    "PENDING" -> "Chờ đối soát thanh toán"
    "PAID" -> "Đã xác nhận nhận tiền"
    "REFUND_REQUIRED" -> "Đang chờ hoàn tiền"
    "REFUNDED" -> "Đã ghi nhận hoàn tiền"
    "CANCELLED" -> "Yêu cầu thanh toán đã hủy"
    else -> "Chưa có yêu cầu thanh toán"
}
