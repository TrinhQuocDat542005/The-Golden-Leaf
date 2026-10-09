package com.example.giaodien.data.model

import java.text.NumberFormat
import java.util.Locale

/** Display only: authoritative totals still come from the server's BigDecimal. */
fun formatVnd(amount: Double): String {
    if (!amount.isFinite()) return "— VND"
    return NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }.format(amount) + " VND"
}
