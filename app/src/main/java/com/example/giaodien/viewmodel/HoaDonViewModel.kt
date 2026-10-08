package com.example.giaodien.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.giaodien.data.model.Payment
import com.example.giaodien.data.model.PaymentQuote
import com.example.giaodien.data.network.RetrofitInstance
import com.example.giaodien.data.repository.HoaDonRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HoaDonViewModel(
    private val repository: HoaDonRepository,
    private val api: com.example.giaodien.data.network.ApiService = RetrofitInstance.api,
    private val session: com.example.giaodien.data.network.AccountSession = com.example.giaodien.data.network.FirebaseAccountSession()
) : ViewModel() {
    var idDat: Long = 0
    var tienBan: Double = 0.0
    var tienAn: Double = 0.0
    private val _trangThaiThanhToan = MutableStateFlow<String?>(null)
    val trangThaiThanhToan: StateFlow<String?> = _trangThaiThanhToan
    private val _processing = MutableStateFlow(false)
    val processing: StateFlow<Boolean> = _processing
    private val _payment = MutableStateFlow<Payment?>(null)
    val payment: StateFlow<Payment?> = _payment
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private val _quote = MutableStateFlow<PaymentQuote?>(null)
    val quote: StateFlow<PaymentQuote?> = _quote
    private var ownerUid = session.uid()
    private var accountGeneration = 0
    private val unsubscribe = session.observe {
        if (session.uid() != ownerUid) {
            accountGeneration++
            ownerUid = session.uid(); idDat = 0; tienBan = 0.0; tienAn = 0.0; _payment.value = null; _quote.value = null; _error.value = null; _trangThaiThanhToan.value = null
        }
    }
    override fun onCleared() { unsubscribe(); super.onCleared() }
    fun loadQuote(id: Long) {
        if (_quote.value?.idDat == id) return
        setThongTinThanhToan(id, 0.0, 0.0)
        _error.value = null
        val uid = session.uid()
        val generation = accountGeneration
        viewModelScope.launch {
            try {
                val result = api.getPaymentQuote(id)
                if (idDat == id && session.uid() == uid && generation == accountGeneration) { tienBan = result.tienBan; tienAn = result.tienAn; _quote.value = result }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (idDat == id && session.uid() == uid && generation == accountGeneration) _error.value = "Chưa lấy được hóa đơn từ server. Vui lòng kiểm tra đơn đã xác nhận và thử lại." }
        }
    }
    fun setThongTinThanhToan(idDat: Long, tienBan: Double, tienAn: Double) {
        if (this.idDat != idDat) { _payment.value = null; _quote.value = null; _trangThaiThanhToan.value = null; _error.value = null }
        this.idDat = idDat; this.tienBan = tienBan; this.tienAn = tienAn
    }
    fun thanhToan(method: String) = load(create = true)
    fun refreshPayment() = load(create = false)
    private fun load(create: Boolean) {
        if (_processing.value || idDat <= 0) return
        val booking = idDat
        val owner = session.uid()
        val generation = accountGeneration
        _processing.value = true; _error.value = null
        viewModelScope.launch {
            try {
                val payment = if (create) api.createPayment(booking) else api.getPayment(booking)
                if (idDat == booking && owner == session.uid() && generation == accountGeneration) {
                    _payment.value = payment; _trangThaiThanhToan.value = payment.status
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (idDat == booking && owner == session.uid() && generation == accountGeneration) _error.value = "Chưa lấy được thông tin thanh toán. Kiểm tra đăng nhập, đơn đã xác nhận và thử lại."
            } finally { _processing.value = false }
        }
    }
}
