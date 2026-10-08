package com.example.giaodien

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.giaodien.data.model.BanSlot
import com.example.giaodien.data.model.BookingAvailability
import com.example.giaodien.ui.screens.NgayGioScreen
import com.example.giaodien.viewmodel.BanSlotViewModel
import java.io.IOException
import java.util.UUID
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Week8InstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val foreground = instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString()
        check(foreground == "com.example.giaodien" || foreground == "com.example.giaodien.test") {
            "Native screenshot is obscured by another window: $foreground"
        }
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            java.io.File(instrumentation.targetContext.filesDir,"week8-$name.png").outputStream().use {
                check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it))
            }
        } finally { bitmap.recycle() }
    }
    private fun slots(): List<BanSlot> {
        val date=BookingAvailability.today().toString()
        // Tomorrow is selected in the screen to avoid time-of-day dependence.
        val tomorrow=BookingAvailability.today().plusDays(1).toString()
        return listOf(BanSlot(1,date,"07:00-11:00",4,0,0),
            BanSlot(2,tomorrow,"07:00-11:00",4,0,0),BanSlot(3,tomorrow,"11:00-15:00",4,4,32))
    }
    @Test fun actualDateScreenBlocksFullSlotAndEnablesAvailableSelection() {
        val vm=BanSlotViewModel { slots() }
        compose.setContent { MaterialTheme { NgayGioScreen(vm,rememberNavController()) } }
        val tomorrow=BookingAvailability.today().plusDays(1)
        compose.onNodeWithTag("booking-date-$tomorrow").performScrollTo().performClick()
        compose.onNodeWithText("Tiếp tục").assertIsNotEnabled()
        compose.onNodeWithText("07:00-11:00").assertIsNotEnabled()
        compose.onNodeWithText("11:00-15:00").performClick()
        compose.onNodeWithText("Tiếp tục").assertIsEnabled()
        compose.onNodeWithText("THE GOLDEN LEAF").assertIsDisplayed()
        screenshot("slot-selection")
    }
    @Test fun actualDateScreenShowsNetworkFailureAndRetry() {
        var fail=true
        val vm=BanSlotViewModel { if(fail) throw IOException("fixture"); slots() }
        compose.setContent { MaterialTheme { NgayGioScreen(vm,rememberNavController()) } }
        compose.onNodeWithText("Không tải được lịch bàn. Kiểm tra kết nối và thử lại.").assertIsDisplayed()
        compose.onNodeWithText("Tiếp tục").assertIsNotEnabled()
        screenshot("network-error")
        fail=false; compose.onNodeWithText("Thử lại").performClick()
        compose.onNodeWithText("Không tải được lịch bàn. Kiểm tra kết nối và thử lại.").assertDoesNotExist()
    }
    @Test fun realDemoApiJourneyFromAndroidPreservesOwnershipAndPendingPayment() {
        val base=InstrumentationRegistry.getArguments().getString("demoBaseUrl") ?: "http://10.0.2.2:18082"
        require(base == "http://10.0.2.2:18082" || base == "http://127.0.0.1:18082") { "Only isolated local demo is allowed" }
        val client=OkHttpClient()
        fun call(path: String, method: String="GET", body: String?=null, token: String?=null, key: String?=null, expected: Int=200): String {
            val request=Request.Builder().url(base+path)
            token?.let { request.header("Authorization","Bearer $it") }; key?.let { request.header("Idempotency-Key",it) }
            request.method(method,body?.toRequestBody("application/json".toMediaType()))
            return client.newCall(request.build()).execute().use { response ->
                val text=response.body!!.string(); assertEquals("$method $path: $text",expected,response.code); text
            }
        }
        assertTrue(JSONObject(call("/api/demo/config")).getBoolean("demo"))
        fun session(persona: String)=JSONObject(call("/api/demo/session","POST","{\"persona\":\"$persona\"}")).getString("token")
        val customer=session("CUSTOMER"); val other=session("OTHER_CUSTOMER")
        val dish=JSONArray(call("/api/thucdon")).getJSONObject(0)
        val menu=dish.getLong("idThucDon")
        call("/api/yeu-thich/add?idThucDon=$menu","POST","",customer)
        assertTrue(JSONArray(call("/api/yeu-thich/list",token=customer)).length()>0)
        assertEquals(0,JSONArray(call("/api/yeu-thich/list",token=other)).length())
        call("/api/binhluan/add","POST","{\"thucDonId\":$menu,\"noiDung\":\"Android synthetic review\",\"rating\":4}",customer)
        val feedback=call("/api/binhluan/$menu",token=customer); assertFalse(feedback.contains("userEmail")); assertFalse(feedback.contains("userUid"))
        val date=BookingAvailability.today().plusDays(1)
        val body="{\"email\":\"customer@example.invalid\",\"ten\":\"Android fixture\",\"ngay\":\"$date\",\"khungGio\":\"11:00-15:00\",\"soLuong\":4,\"ghiChu\":\"Synthetic test\",\"viTriBan\":\"Trong nhà\"}"
        val key=UUID.randomUUID().toString(); val id=JSONObject(call("/api/datban/save","POST",body,customer,key)).getLong("idDat")
        assertEquals(id,JSONObject(call("/api/datban/save","POST",body,customer,key)).getLong("idDat"))
        call("/api/datban/$id",token=other,expected=403)
        val cart=JSONArray().put(JSONObject().put("idDat",id).put("idThucDon",menu)
            .put("tenMon",dish.getString("tenMon")).put("soLuong",2).put("giaMon",dish.getDouble("gia")))
        call("/api/giohang/$id","PUT",cart.toString(),customer)
        call("/api/datban/$id/confirm","POST","",customer)
        val quote=JSONObject(call("/api/payments/bookings/$id/quote",token=customer))
        val payment=JSONObject(call("/api/payments/bookings/$id","POST","",customer))
        assertEquals("PENDING",payment.getString("status")); assertTrue(payment.isNull("paidAt"))
        assertEquals(quote.getDouble("tongTien"),payment.getDouble("amount"),0.001)
        assertEquals("NOT-A-REAL-ACCOUNT",payment.getString("accountNumber"))
        call("/api/datban/$id/cancel","POST","",customer)
        assertEquals("CANCELLED",JSONObject(call("/api/payments/bookings/$id",token=customer)).getString("status"))
    }
}
