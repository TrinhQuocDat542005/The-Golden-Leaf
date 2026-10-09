package com.example.giaodien

import com.example.giaodien.data.model.*
import com.example.giaodien.data.network.*
import com.example.giaodien.data.repository.*
import com.example.giaodien.ui.viewmodel.*
import com.example.giaodien.viewmodel.ChiTietHoaDonViewModel
import com.example.giaodien.viewmodel.ChiTietHoaDonState
import java.io.IOException
import java.lang.reflect.Proxy
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class Week9AccountTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }
    private class Session : AccountSession {
        var owner: String? = "a"
        private val listeners = mutableListOf<() -> Unit>()
        override fun uid() = owner
        override fun observe(onChange: () -> Unit): () -> Unit { listeners.add(onChange); onChange(); return { listeners.remove(onChange) } }
        fun switch(uid: String?) { owner = uid; listeners.toList().forEach { it() } }
    }
    private fun api(call: (String) -> Any?): ApiService = Proxy.newProxyInstance(ApiService::class.java.classLoader,
        arrayOf(ApiService::class.java)) { _, method, _ -> call(method.name) } as ApiService
    private fun booking(id: Long) = LichSuDonDayDuDTO(idDat=id, email="a@example.invalid", ten="Fixture", ngay="2026-10-09",
        khungGio="11:00-15:00", soLuong=4, viTriBan="Trong nhà")

    @Test fun historyFailureRetryAndLogoutAreVisibleAndIsolated() = runTest(dispatcher) {
        val session = Session(); var fail = true
        val service = api { if (fail) throw IOException(); listOf(booking(1)) }
        val vm = TaiKhoanViewModel(TaiKhoanRepository(service),session)
        advanceUntilIdle(); assertNotNull(vm.errorMessage.value); assertFalse(vm.isLoading.value)
        fail = false; vm.loadData(); advanceUntilIdle()
        assertNull(vm.errorMessage.value); assertEquals(1L,vm.lichSuDonDat.value.single().idDat)
        session.switch(null); assertTrue(vm.lichSuDonDat.value.isEmpty()); assertTrue(vm.choXacNhan.value.isEmpty())
        assertFalse(vm.isLoading.value)
    }
    @Test fun reentrantAccountSwitchRejectsOldHistoryResponse() = runTest(dispatcher) {
        val session = Session(); var switched = false
        val service = api { name ->
            if (!switched && name == "getChoXacNhan") { switched = true; session.switch("b"); listOf(booking(1)) }
            else listOf(booking(if (session.owner == "b") 2 else 1))
        }
        val vm = TaiKhoanViewModel(TaiKhoanRepository(service),session)
        advanceUntilIdle(); assertEquals(2L,vm.choXacNhan.value.single().idDat)
        assertEquals(2L,vm.lichSuDonDat.value.single().idDat); assertFalse(vm.isLoading.value)
    }
    @Test fun inboxErrorHasRetryAndDeduplicatesSuccessfulResult() = runTest(dispatcher) {
        val session = Session(); var fail = true
        val service = api { name ->
            if (fail) throw IOException()
            if (name == "getUnreadNotificationCount") UnreadNotificationCount(1)
            else listOf(Notification(1,message="Fixture"),Notification(1,message="Fixture"))
        }
        val vm = NotificationViewModel(NotificationRepository(service),session)
        runCurrent(); assertNotNull(vm.errorMessage.value); assertFalse(vm.loading.value)
        fail = false; vm.retry(); runCurrent()
        assertNull(vm.errorMessage.value); assertEquals(1,vm.notifications.value.size); assertEquals(1,vm.unreadCount.value)
        session.switch(null); runCurrent(); assertTrue(vm.notifications.value.isEmpty()); assertEquals(0,vm.unreadCount.value)
    }
    @Test fun accountSwitchStopsOldInboxPolling() = runTest(dispatcher) {
        val session = Session(); var aCalls = 0
        val service = api { name ->
            if (session.owner == "a") aCalls++
            if (name == "getUnreadNotificationCount") UnreadNotificationCount(1)
            else listOf(Notification(if (session.owner == "a") 1 else 2,message="Fixture"))
        }
        val vm = NotificationViewModel(NotificationRepository(service),session)
        runCurrent(); val initial = aCalls
        session.switch("b"); runCurrent(); assertEquals(2L,vm.notifications.value.single().id)
        advanceTimeBy(15001); runCurrent(); assertEquals(initial,aCalls)
        session.switch(null); runCurrent()
    }
    @Test fun invoiceErrorIsSanitizedAndRetryWorks() = runTest(dispatcher) {
        val session = Session(); var fail = true
        val service = api { if (fail) throw IOException("private@example.invalid raw backend error"); booking(1) }
        val vm = ChiTietHoaDonViewModel(TaiKhoanRepository(service),session)
        vm.loadChiTietHoaDon(1); advanceUntilIdle()
        val error = vm.state.value as ChiTietHoaDonState.Error
        assertFalse(error.message.contains("private@")); assertFalse(error.message.contains("raw backend"))
        fail = false; vm.loadChiTietHoaDon(1); advanceUntilIdle()
        assertEquals(1L,(vm.state.value as ChiTietHoaDonState.Success).data.idDat)
    }
    @Test fun invoiceResponseFromOldAccountIsDiscarded() = runTest(dispatcher) {
        val session = Session()
        val service = api { session.switch("b"); booking(1) }
        val vm = ChiTietHoaDonViewModel(TaiKhoanRepository(service),session)
        vm.loadChiTietHoaDon(1); advanceUntilIdle()
        assertFalse(vm.state.value is ChiTietHoaDonState.Success)
    }
}
