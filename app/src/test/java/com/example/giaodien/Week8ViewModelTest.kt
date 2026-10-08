package com.example.giaodien

import androidx.lifecycle.SavedStateHandle
import com.example.giaodien.data.model.*
import com.example.giaodien.data.network.*
import com.example.giaodien.data.repository.*
import com.example.giaodien.viewmodel.*
import java.io.IOException
import java.lang.reflect.Proxy
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class Week8ViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }
    private val dish = ThucDon(1, "Soup", 50000.0)
    private val review = BinhLuan(1, ThucDonRef(1), "Khách hàng", 4, "Good", "2026-10-08T00:00:00Z")
    private val request = DatBan(email="a@example.invalid",ten="A",ngay="2026-10-09",khungGio="11:00-15:00",soLuong=4,ghiChu="",viTriBan="Trong nhà")
    private class Session : AccountSession {
        var owner: String? = "a"
        private val listeners = mutableListOf<() -> Unit>()
        override fun uid() = owner
        override fun observe(onChange: () -> Unit): () -> Unit { listeners.add(onChange); onChange(); return { listeners.remove(onChange) } }
        fun switch(uid: String?) { owner=uid; listeners.toList().forEach { it() } }
    }
    private class Reviews : ReviewRepository {
        var fail=false; var writes=0
        var result: List<BinhLuan> = emptyList()
        var pending: CompletableDeferred<List<BinhLuan>>?=null
        override suspend fun list(id: Long): List<BinhLuan> { if(fail) throw IOException(); return pending?.await() ?: result }
        override suspend fun save(id: Long,content: String,rating: Int): BinhLuan { writes++; if(fail) throw IOException(); return BinhLuan(1,ThucDonRef(id),"Khách hàng",rating,content,"") }
    }
    private class Favorites : FavoriteRepository {
        var items: List<ThucDon> = emptyList(); var fail=false; var writes=0
        var pending: CompletableDeferred<List<ThucDon>>?=null
        override suspend fun list(): List<ThucDon> { if(fail) throw IOException(); return pending?.await() ?: items }
        override suspend fun add(id: Long) { writes++; items=listOf(ThucDon(id)) }
        override suspend fun remove(id: Long) { writes++; items=emptyList() }
    }
    private fun api(call: (String, Array<out Any?>) -> Any?): ApiService = Proxy.newProxyInstance(
        ApiService::class.java.classLoader,arrayOf(ApiService::class.java)
    ) { _, method, args -> call(method.name,args ?: emptyArray()) } as ApiService

    @Test fun reviewFailureIsStateNotUnhandledExceptionAndRetryWorks() = runTest(dispatcher) {
        val repo=Reviews(); val vm=BinhLuanViewModel(repo,Session()); repo.fail=true
        vm.loadBinhLuan(1); advanceUntilIdle(); assertNotNull(vm.error.value); assertFalse(vm.loading.value)
        repo.fail=false; repo.result=listOf(review); vm.loadBinhLuan(1); advanceUntilIdle()
        assertNull(vm.error.value); assertEquals(1,vm.binhLuanList.value.size)
    }
    @Test fun failedReviewDoesNotClearDraftAndSuccessfulRetryDoes() = runTest(dispatcher) {
        val repo=Reviews(); val vm=BinhLuanViewModel(repo,Session()); var cleared=false
        repo.fail=true; vm.addBinhLuan(1,"Draft",4) { cleared=true }; advanceUntilIdle()
        assertFalse(cleared); assertNotNull(vm.error.value); assertFalse(vm.sending.value)
        repo.fail=false; vm.addBinhLuan(1,"Draft",4) { cleared=true }; advanceUntilIdle(); assertTrue(cleared)
    }
    @Test fun invalidReviewAndLoggedOutWritesAreBlocked() = runTest(dispatcher) {
        val repo=Reviews(); val session=Session(); val vm=BinhLuanViewModel(repo,session)
        for(content in listOf("", "x".repeat(2001))) vm.addBinhLuan(1,content,5)
        vm.addBinhLuan(1,"Valid",0); session.switch(null); vm.addBinhLuan(1,"Valid",5)
        advanceUntilIdle(); assertEquals(0,repo.writes)
    }
    @Test fun doubleTapReviewMakesOneWrite() = runTest(dispatcher) {
        val repo=Reviews(); val vm=BinhLuanViewModel(repo,Session())
        vm.addBinhLuan(1,"Draft",5); vm.addBinhLuan(1,"Draft",5); advanceUntilIdle(); assertEquals(1,repo.writes)
    }
    @Test fun oldReviewResponseCannotReturnAfterAccountSwitch() = runTest(dispatcher) {
        val repo=Reviews(); repo.pending=CompletableDeferred(); val session=Session(); val vm=BinhLuanViewModel(repo,session)
        vm.loadBinhLuan(1); runCurrent(); session.switch("b"); repo.pending!!.complete(listOf(review)); advanceUntilIdle()
        assertTrue(vm.binhLuanList.value.isEmpty()); assertNull(vm.error.value)
    }
    @Test fun olderDishResponseCannotOverwriteNewDish() = runTest(dispatcher) {
        val repo=Reviews(); val pending=CompletableDeferred<List<BinhLuan>>(); repo.pending=pending
        val vm=BinhLuanViewModel(repo,Session()); vm.loadBinhLuan(1); runCurrent()
        repo.pending=null; vm.loadBinhLuan(2); runCurrent(); pending.complete(listOf(review)); advanceUntilIdle()
        assertTrue(vm.binhLuanList.value.isEmpty())
    }
    @Test fun favoritesFailureRetryAndToggleAreObservable() = runTest(dispatcher) {
        val repo=Favorites(); val vm=YeuThichViewModel(repo,Session()); repo.fail=true
        vm.loadFavorites("forged-uid"); advanceUntilIdle(); assertNotNull(vm.error.value)
        repo.fail=false; vm.toggleFavorite("forged-uid",dish); advanceUntilIdle(); assertEquals(1,vm.favoriteList.value.size)
        vm.toggleFavorite("forged-uid",dish); advanceUntilIdle(); assertTrue(vm.favoriteList.value.isEmpty()); assertNull(vm.error.value)
    }
    @Test fun favoriteDoubleTapIsSerialized() = runTest(dispatcher) {
        val repo=Favorites(); val vm=YeuThichViewModel(repo,Session())
        vm.toggleFavorite("a",dish); vm.toggleFavorite("a",dish); advanceUntilIdle(); assertEquals(1,repo.writes)
    }
    @Test fun favoritesClearImmediatelyOnLogoutAndDiscardOldResponse() = runTest(dispatcher) {
        val repo=Favorites(); repo.items=listOf(dish); val session=Session(); val vm=YeuThichViewModel(repo,session)
        vm.loadFavorites("a"); advanceUntilIdle(); assertEquals(1,vm.favoriteList.value.size)
        repo.pending=CompletableDeferred(); vm.loadFavorites("a"); runCurrent(); session.switch(null)
        assertTrue(vm.favoriteList.value.isEmpty()); repo.pending!!.complete(listOf(dish)); advanceUntilIdle(); assertTrue(vm.favoriteList.value.isEmpty())
    }
    @Test fun slotsNetworkFailureClearsStaleInventoryAndCanRetry() = runTest(dispatcher) {
        var fail=true; var calls=0
        val vm=BanSlotViewModel { calls++; if(fail) throw IOException(); emptyList() }
        vm.fetchBanSlots(); advanceUntilIdle(); assertEquals(1,calls); assertNotNull(vm.error.value)
        fail=false; vm.fetchBanSlots(); advanceUntilIdle(); assertNull(vm.error.value); assertFalse(vm.loading.value)
    }
    @Test fun menuFailureIsVisibleAndRetryRecovers() = runTest(dispatcher) {
        var fail=true; val vm=ThucDonViewModel { if(fail) throw IOException(); listOf(dish) }
        vm.loadThucDon(); advanceUntilIdle(); assertNotNull(vm.error.value)
        fail=false; vm.loadThucDon(); advanceUntilIdle(); assertEquals(1,vm.thucDonList.value.size); assertNull(vm.error.value)
    }
    @Test fun weatherWithoutKeyDoesNotContactProvider() = runTest(dispatcher) {
        val vm=WeatherViewModel("") { _, _ -> fail("Provider must not be called"); null }
        vm.loadForecast(); advanceUntilIdle(); assertNotNull(vm.message.value)
    }
    @Test fun weatherNetworkFailureDoesNotCrashBooking() = runTest(dispatcher) {
        val vm=WeatherViewModel("fixture-key") { _, _ -> throw IOException() }
        vm.loadForecast(); advanceUntilIdle(); assertNull(vm.forecast.value); assertNotNull(vm.message.value)
    }
    @Test fun timezoneUsesRestaurantDateEvenWhenDeviceDateDiffers() {
        val clock=Clock.fixed(Instant.parse("2026-10-08T18:00:00Z"),ZoneOffset.ofHours(-8))
        assertEquals("2026-10-09",BookingAvailability.today(clock).toString())
    }
    @Test fun availabilityRejectsStartedFullMalformedAndOutOfWindowSlots() {
        val clock=Clock.fixed(Instant.parse("2026-10-08T04:00:00Z"),ZoneOffset.UTC)
        val slot=BanSlot(1,"2026-10-08","11:00-15:00",4,4,32)
        assertFalse(BookingAvailability.selectable(slot,clock)) // exact start
        assertTrue(BookingAvailability.selectable(slot.copy(khungGio="15:00-19:00"),clock))
        assertFalse(BookingAvailability.selectable(slot.copy(ngay="2026-10-09",soBanConLai=0),clock))
        assertFalse(BookingAvailability.selectable(slot.copy(ngay="2026-10-15"),clock))
        assertFalse(BookingAvailability.selectable(slot.copy(khungGio="broken"),clock))
    }
    @Test fun bookingNetworkRetryKeepsSameIdempotencyKeyAndSavedIntent() = runTest(dispatcher) {
        val keys=mutableListOf<String>(); var fail=true
        val service=api { name,args -> when(name) {
            "createDatBan" -> { keys.add(args[1] as String); if(fail) throw IOException(); request.copy(idDat=9) }
            else -> error(name)
        } }
        val state=SavedStateHandle(); val session=Session()
        val vm=DatBanViewModel(state,DatBanRepository(service),session)
        vm.datBan(request,"intent",{},{}); advanceUntilIdle(); fail=false
        vm.datBan(request,"intent",{},{}); advanceUntilIdle()
        assertEquals(keys[0],keys[1]); assertEquals(9L,vm.currentDatBan?.idDat)
        assertEquals(9L,state.get<Long>("currentBookingId"))
    }
    @Test fun expiredBookingReplayResetsKeyForNextAttempt() = runTest(dispatcher) {
        val keys=mutableListOf<String>(); val service=api { name,args ->
            check(name=="createDatBan"); keys.add(args[1] as String); request.copy(idDat=9,status="EXPIRED")
        }
        val vm=DatBanViewModel(SavedStateHandle(),DatBanRepository(service),Session())
        repeat(2) { vm.datBan(request,"intent",{},{}); advanceUntilIdle() }
        assertNotEquals(keys[0],keys[1]); assertNull(vm.currentDatBan); assertNotNull(vm.error.value)
    }
    @Test fun bookingDoubleTapMakesOneRequest() = runTest(dispatcher) {
        var calls=0; val service=api { _,_ -> calls++; request.copy(idDat=9) }
        val vm=DatBanViewModel(SavedStateHandle(),DatBanRepository(service),Session())
        repeat(2) { vm.datBan(request,"intent",{},{}) }; advanceUntilIdle(); assertEquals(1,calls)
    }
    @Test fun staleBookingErrorCannotReachNewAccount() = runTest(dispatcher) {
        val pending=CompletableDeferred<DatBan>(); val session=Session()
        val repo=object : DatBanRepository(api { _,_ -> error("unused") }) {
            override suspend fun datBan(datBan: DatBan,key: String)=pending.await()
        }
        val vm=DatBanViewModel(SavedStateHandle(),repo,session); var notified=false
        vm.datBan(request,"intent",{}, { notified=true }); runCurrent(); session.switch("b"); session.switch("a")
        pending.completeExceptionally(IOException()); advanceUntilIdle(); assertFalse(notified); assertNull(vm.error.value)
    }
    @Test fun bookingCartQuotePendingAndCancellationJourney() = runTest(dispatcher) {
        val calls=mutableListOf<String>(); val session=Session()
        val service=api { name,args -> calls.add(name); when(name) {
            "createDatBan" -> request.copy(idDat=9,holdExpiresAt="2026-10-09T05:00:00Z")
            "replaceGioHang" -> listOf(GioHangResponse(1,9,1,"Soup snapshot",2,49000.0,98000.0))
            "confirmDatBan" -> request.copy(idDat=9,status="CONFIRMED")
            "getPaymentQuote" -> PaymentQuote(9,200000.0,98000.0,298000.0,"VND")
            "createPayment" -> Payment(1,9,298000.0,"VND","PENDING","TGL9","DEMO","NOT-A-REAL-ACCOUNT","DEMO")
            "cancelDatBan" -> request.copy(idDat=9,status="CANCELLED")
            else -> error("Unexpected $name with ${args.size} args")
        } }
        val booking=DatBanViewModel(SavedStateHandle(),DatBanRepository(service),session)
        val cart=GioHangViewModel(service,SavedStateHandle(),session)
        val payment=HoaDonViewModel(HoaDonRepository(service),service,session)
        booking.datBan(request,"intent",{ cart.setDatBanId(it.idDat!!,it.holdExpiresAt) },{ fail(it) }); advanceUntilIdle()
        cart.addToCart(dish); cart.addToCart(dish); var confirmed=false
        cart.xacNhanDatMon({ confirmed=true },{ fail(it) }); advanceUntilIdle(); assertTrue(confirmed)
        assertEquals(49000.0,cart.gioHangList.value.single().thucDon.gia,0.001); assertNull(cart.holdExpiresAt.value)
        payment.loadQuote(9); advanceUntilIdle(); assertEquals(298000.0,payment.quote.value!!.tongTien,0.001)
        payment.thanhToan("BANK_TRANSFER"); advanceUntilIdle(); assertEquals("PENDING",payment.payment.value!!.status); assertNull(payment.payment.value!!.paidAt)
        cart.cancelBooking({}, { fail(it) }); advanceUntilIdle(); assertTrue(cart.gioHangList.value.isEmpty()); assertNull(cart.currentDatBanId.value)
        assertEquals(listOf("createDatBan","replaceGioHang","confirmDatBan","getPaymentQuote","createPayment","cancelDatBan"),calls)
    }
    @Test fun cartExpiryErrorRetainsCartAndDoesNotConfirm() = runTest(dispatcher) {
        val service=api { name,_ -> check(name=="replaceGioHang"); throw IOException("fixture expiry") }
        val cart=GioHangViewModel(service,SavedStateHandle(),Session()); cart.setDatBanId(9); cart.addToCart(dish)
        var error:String?=null; cart.xacNhanDatMon({ fail("Must not confirm") },{ error=it }); advanceUntilIdle()
        assertNotNull(error); assertEquals(1,cart.gioHangList.value.size); assertFalse(cart.submitting.value)
    }
    @Test fun accountSwitchClearsBookingCartAndPaymentMoney() = runTest(dispatcher) {
        val session=Session(); val service=api { _,_ -> request.copy(idDat=9) }
        val booking=DatBanViewModel(SavedStateHandle(),DatBanRepository(service),session)
        val cart=GioHangViewModel(service,SavedStateHandle(),session)
        val payment=HoaDonViewModel(HoaDonRepository(service),service,session)
        booking.setDatBan(request.copy(idDat=9)); cart.setDatBanId(9); cart.addToCart(dish); payment.setThongTinThanhToan(9,200000.0,50000.0)
        session.switch("b"); assertNull(booking.currentDatBan); assertNull(cart.currentDatBanId.value); assertTrue(cart.gioHangList.value.isEmpty())
        assertEquals(0,payment.idDat); assertEquals(0.0,payment.tienBan,0.0); assertEquals(0.0,payment.tienAn,0.0)
    }
}
