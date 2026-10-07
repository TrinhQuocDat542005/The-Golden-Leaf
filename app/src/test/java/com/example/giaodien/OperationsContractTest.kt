package com.example.giaodien

import com.example.giaodien.data.model.*
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class OperationsContractTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val payment = """{"id":7,"bookingId":3,"amount":200000.50,"currency":"VND","status":"PENDING","reference":"TGL3","bankName":"Test bank","accountNumber":"123456789","accountName":"TEST RESTAURANT","paidAt":null}"""
    @Test fun pendingPaymentIsNotInterpretedAsSuccess() {
        val result = json.decodeFromString<Payment>(payment)
        assertEquals("PENDING", result.status); assertNull(result.paidAt)
        assertEquals(200000.50, result.amount, 0.001)
    }
    @Test fun paidAndRefundStatesRemainDistinct() {
        for (status in listOf("PAID", "REFUND_REQUIRED", "REFUNDED", "CANCELLED"))
            assertEquals(status, json.decodeFromString<Payment>(payment.replace("PENDING", status)).status)
    }
    @Test fun notificationContractUsesReadFlagAndNullableBooking() {
        val result = json.decodeFromString<Notification>("""{"id":1,"bookingId":null,"type":"PAID","title":"The Golden Leaf","message":"Đã xác nhận thanh toán","status":"CREATED","readFlag":false,"createdAt":"2026-10-07T03:00:00Z"}""")
        assertFalse(result.readFlag); assertNull(result.bookingId)
    }
    @Test fun historyIncludesBookingLifecycleAndActualPaymentState() {
        val result = json.decodeFromString<LichSuDonDayDuDTO>("""{"idDat":3,"email":"guest@example.com","ten":"Guest","ngay":"2026-10-07","khungGio":"11:00-15:00","soLuong":8,"viTriBan":"Trong nhà","status":"CANCELLED","paymentStatus":"REFUND_REQUIRED","danhSachMon":[]}""")
        assertEquals("CANCELLED", result.status); assertEquals("REFUND_REQUIRED", result.paymentStatus)
    }
    @Test fun historyAcceptsRealServerItemSnapshot() {
        val result = json.decodeFromString<LichSuDonDayDuDTO>("""{"idDat":3,"email":"guest@example.com","ten":"Guest","ngay":"2026-10-07","khungGio":"11:00-15:00","soLuong":8,"viTriBan":"Trong nhà","danhSachMon":[{"id":1,"idDat":3,"idThucDon":2,"email":"guest@example.com","tenMon":"Soup","soLuong":2,"giaMon":50000,"thanhTien":100000}]}""")
        assertEquals("Soup", result.danhSachMon.single().tenMon)
    }
    @Test fun unknownBookingStateCannotBePresentedAsConfirmed() {
        assertEquals("Đang cập nhật", bookingStatusLabel("UNKNOWN"))
        assertNotEquals(paymentStatusLabel("PENDING"), paymentStatusLabel("PAID"))
    }
    @Test fun unreadCountSupportsMoreThanFeedPageSize() {
        assertEquals(150, json.decodeFromString<UnreadNotificationCount>("""{"count":150}""").count)
    }
    @Test fun quoteContractDoesNotRequirePersistedInvoiceId() {
        val result = json.decodeFromString<PaymentQuote>("""{"idDat":3,"tienBan":300000,"tienAn":125000.50,"tongTien":425000.50,"currency":"VND"}""")
        assertEquals(300000.0, result.tienBan, 0.001); assertEquals(425000.50, result.tongTien, 0.001)
    }
    @Test fun menuImagePathsFollowConfiguredApiInsteadOfEmulatorHost() {
        val base = "https://restaurant.example/"
        assertEquals("https://restaurant.example/uploads/new.png", menuImageUrl("/uploads/new.png", base))
        assertEquals("https://restaurant.example/uploads/old.jpg", menuImageUrl("old.jpg", base))
        assertEquals("https://cdn.example/photo.png", menuImageUrl("https://cdn.example/photo.png", base))
        assertEquals("", menuImageUrl("", base))
    }
}
