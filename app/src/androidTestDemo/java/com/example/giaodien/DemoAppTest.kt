package com.example.giaodien

import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.giaodien.data.model.BookingAvailability
import com.example.giaodien.data.network.DemoSession
import com.google.firebase.FirebaseApp
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File
import java.util.concurrent.TimeUnit

/** Customer UI runs real MainActivity/MyApp; staff operations use the real, synthetic API. */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class DemoAppTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build()
    @Before fun resetSession() { compose.runOnIdle { DemoSession.signOut() }; assertTrue(BuildConfig.DEMO_MODE) }
    @After fun logout() { compose.runOnIdle { DemoSession.signOut() } }

    private fun api(path: String, method: String = "GET", body: String? = null, token: String? = DemoSession.state?.token): String {
        val builder = Request.Builder().url(BuildConfig.API_BASE_URL + path)
        token?.let { builder.header("Authorization", "Bearer $it") }
        builder.method(method, if (method == "GET") null else (body ?: "").toRequestBody("application/json".toMediaType()))
        return client.newCall(builder.build()).execute().use { response ->
            val text = response.body!!.string()
            assertEquals("$method $path must succeed", 200, response.code)
            text
        }
    }
    private fun waitTag(tag: String) { compose.waitUntil(25000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() } }
    private fun clickText(text: String) {
        compose.waitUntil(25000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().any { !it.config.contains(SemanticsProperties.Disabled) } }
        val node = compose.onNodeWithText(text)
        try { node.assertIsDisplayed() } catch (_: AssertionError) { node.performScrollTo() }
        node.performClick()
    }
    private fun login(other: Boolean = false) {
        val text = if (other) "Khách khác · kiểm tra tài khoản riêng" else "Khách demo · có đơn mẫu"
        compose.waitUntil(15000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(text).performClick()
        compose.waitUntil(25000) { compose.onAllNodesWithTag("open-menu").fetchSemanticsNodes().isNotEmpty() ||
            compose.onAllNodesWithTag("demo-login-error").fetchSemanticsNodes().isNotEmpty() }
        check(compose.onAllNodesWithTag("demo-login-error").fetchSemanticsNodes().isEmpty()) {
            "Demo login failed (sanitized metadata): ${DemoSession.failureTrace}"
        }
        assertEquals(if (other) "demo-other" else "demo-customer", DemoSession.state?.identity?.uid)
        assertTrue("Demo must not initialize Firebase", FirebaseApp.getApps(compose.activity).isEmpty())
    }
    private fun account() {
        compose.onNodeWithContentDescription("Tài khoản").performClick()
        compose.onNodeWithText(DemoSession.state!!.identity.email!!).assertIsDisplayed()
    }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val windows = ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("dumpsys window"))
            .bufferedReader().use { it.readText() }
        File(instrumentation.targetContext.filesDir, "week9-$name-window.txt").writeText(windows)
        val focus = windows.lineSequence().firstOrNull { it.contains("mCurrentFocus=") }
        check(focus != null && Regex("\\bcom\\.example\\.giaodien\\.demo/").containsMatchIn(focus)) { "Screenshot focus is not the demo app: $focus" }
        var bitmap: Bitmap? = null
        val deadline = android.os.SystemClock.uptimeMillis() + 10000
        while (bitmap == null && android.os.SystemClock.uptimeMillis() < deadline) {
            val frame = instrumentation.uiAutomation.takeScreenshot()
            if (frame != null) {
                val pixels = if (android.os.Build.VERSION.SDK_INT >= 26 && frame.config == Bitmap.Config.HARDWARE)
                    checkNotNull(frame.copy(Bitmap.Config.ARGB_8888, false)) else frame
                val colors = mutableMapOf<Int, Int>()
                try {
                    for (x in 1..63) for (y in 8..55) {
                        val color = pixels.getPixel(pixels.width*x/64, pixels.height*y/64)
                        colors[color] = (colors[color] ?: 0) + 1
                    }
                } finally { if (pixels !== frame) pixels.recycle() }
                if (colors.size >= 6 && (colors.values.maxOrNull() ?: 0) < 63*48*0.97) bitmap = frame else frame.recycle()
            }
            if (bitmap == null) android.os.SystemClock.sleep(100)
        }
        val rendered = checkNotNull(bitmap) { "Blank native frame" }
        try { File(instrumentation.targetContext.filesDir,"week9-$name.png").outputStream().use { check(rendered.compress(Bitmap.CompressFormat.PNG,100,it)) } }
        finally { rendered.recycle() }
    }

    @Test fun aBookingPaymentAndHistoryThroughRealCustomerUI() {
        login(); screenshot("home")
        val customer = DemoSession.state!!.token
        val menu = JSONArray(api("api/thucdon"))
        val salad = (0 until menu.length()).map { menu.getJSONObject(it) }.first { it.getString("tenMon") == "Salad vườn xanh" }
        val dish = salad.getLong("idThucDon")
        compose.onNodeWithContentDescription("Đặt bàn").performClick()
        val tomorrow = BookingAvailability.today().plusDays(1)
        waitTag("booking-date-$tomorrow")
        compose.onNodeWithTag("booking-date-$tomorrow").performScrollTo().performClick()
        clickText("11:00-15:00"); clickText("Tiếp tục"); clickText("Tiếp tục"); clickText("Tiếp tục")
        waitTag("guests-plus")
        repeat(3) { compose.onNodeWithTag("guests-plus").performScrollTo().performClick() }
        compose.onNodeWithTag("booking-submit").performScrollTo().assertIsEnabled().performClick()
        waitTag("choose-dish-$dish")
        repeat(2) { compose.onAllNodesWithTag("choose-dish-$dish").onFirst().performScrollTo().performClick() }
        compose.onNodeWithContentDescription("Giỏ hàng").performClick()
        clickText("Xác Nhận Đặt Món")
        clickText("Chọn Phương Thức Thanh Toán"); clickText("Ngân hàng"); clickText("Xác nhận")
        compose.waitUntil(25000) { compose.onAllNodesWithText("NOT-A-REAL-ACCOUNT", substring = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("340.000 VND").assertExists()
        val pending = JSONArray(api("api/taikhoan/choXacNhan", token = customer))
        val booking = (0 until pending.length()).map { pending.getJSONObject(it) }.first {
            it.getString("ngay").take(10) == tomorrow.toString() && it.getString("khungGio") == "11:00-15:00" && it.getString("status") == "CONFIRMED"
        }
        val id = booking.getLong("idDat")
        val payment = JSONObject(api("api/payments/bookings/$id", token = customer))
        assertEquals("PENDING", payment.getString("status")); assertEquals(340000.0, payment.getDouble("amount"), 0.001)
        screenshot("payment")
        val staff = JSONObject(api("api/demo/session", "POST", "{\"persona\":\"STAFF\"}", null)).getString("token")
        api("api/staff/bookings/$id/verify-payment", "POST", "{\"reference\":\"NATIVE-DEMO-$id\",\"amount\":340000}", staff)
        val tableId = JSONArray(api("api/staff/tables", token = staff)).getJSONObject(0).getLong("id")
        api("api/staff/bookings/$id/assign", "POST", "{\"tableIds\":[$tableId]}", staff)
        clickText("Kiểm tra trạng thái")
        compose.waitUntil(20000) { compose.onAllNodesWithText("Nhà hàng đã xác nhận nhận tiền.").fetchSemanticsNodes().isNotEmpty() }
        clickText("Về trang chủ"); account()
        waitTag("booking-history-$id")
        val history = compose.onNodeWithTag("booking-history-$id")
        try { history.assertIsDisplayed() } catch (_: AssertionError) { history.performScrollTo() }
        history.assertIsDisplayed()
        screenshot("history")
        compose.onNodeWithTag("booking-details-$id").performClick()
        waitTag("invoice-content")
        compose.onNodeWithTag("invoice-content").performScrollToNode(hasText("TỔNG TIỀN"))
        compose.onNodeWithText(com.example.giaodien.ui.screens.formatMoney(340000.0)).assertIsDisplayed()
        screenshot("invoice")
        assertEquals("ASSIGNED", JSONObject(api("api/datban/$id", token = customer)).getString("status"))
        assertTrue(FirebaseApp.getApps(compose.activity).isEmpty())
    }

    @Test fun bFavoritesAndReviewPersistViaNativeUI() {
        login(other = true)
        compose.onNodeWithTag("open-menu").performScrollTo().performClick()
        val menu = JSONArray(api("api/thucdon"))
        val id = (0 until menu.length()).map { menu.getJSONObject(it) }.first { it.getString("tenMon") == "Salad vườn xanh" }.getLong("idThucDon")
        waitTag("favorite-dish-$id")
        compose.onNodeWithTag("favorite-dish-$id").performScrollTo().performClick()
        compose.waitUntil(15000) { JSONArray(api("api/yeu-thich/list")).length() == 1 }
        screenshot("menu")
        compose.onNodeWithTag("menu-dish-$id").performClick()
        waitTag("review-draft")
        val draft = "Đánh giá qua Android demo tuần 9 — dữ liệu tổng hợp."
        compose.onNodeWithTag("review-draft").performScrollTo().performTextInput(draft)
        // Only the disposable emulator keyboard, no app-navigation injection.
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("input keyevent 4").close()
        compose.onNodeWithTag("review-submit").performScrollTo().assertIsEnabled().performClick()
        compose.waitUntil(15000) { api("api/binhluan/$id").contains(draft) }
        compose.onNodeWithTag("review-draft").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.EditableText, androidx.compose.ui.text.AnnotatedString("")))
        screenshot("review")
        val feedback = api("api/binhluan/$id")
        assertFalse(feedback.contains("userEmail")); assertFalse(feedback.contains("userUid"))
    }

    @Test fun cLogoutAndAccountSwitchKeepOtherHistoryPrivate() {
        login(); account(); clickText("Đăng xuất")
        login(other = true); account()
        assertEquals(0, JSONArray(api("api/taikhoan/choXacNhan")).length())
        assertEquals(0, JSONArray(api("api/taikhoan/lichSuDonDat")).length())
        compose.onNodeWithText("other@example.invalid").assertIsDisplayed()
        clickText("Đăng xuất"); login()
        assertTrue(JSONArray(api("api/taikhoan/lichSuDonDat")).length() >= 2)
        assertTrue(FirebaseApp.getApps(compose.activity).isEmpty())
    }
}
