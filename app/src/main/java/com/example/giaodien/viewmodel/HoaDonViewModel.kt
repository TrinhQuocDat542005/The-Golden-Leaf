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

class HoaDonViewModel(private val repository: HoaDonRepository) : ViewModel() {
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
    private val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    private var ownerUid = auth.currentUser?.uid
    private val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener {
        if (it.currentUser?.uid != ownerUid) {
            ownerUid = it.currentUser?.uid; idDat = 0; _payment.value = null; _quote.value = null; _error.value = null; _trangThaiThanhToan.value = null
        }
    }
    init { auth.addAuthStateListener(listener) }
    override fun onCleared() { auth.removeAuthStateListener(listener); super.onCleared() }
    fun loadQuote(id: Long) {
        if (_quote.value?.idDat == id) return
        setThongTinThanhToan(id, 0.0, 0.0)
        _error.value = null
        val uid = auth.currentUser?.uid
        viewModelScope.launch {
            try {
                val result = RetrofitInstance.api.getPaymentQuote(id)
                if (idDat == id && auth.currentUser?.uid == uid) { tienBan = result.tienBan; tienAn = result.tienAn; _quote.value = result }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (idDat == id && auth.currentUser?.uid == uid) _error.value = "Chưa lấy được hóa đơn từ server. Vui lòng kiểm tra đơn đã xác nhận và thử lại." }
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
        val owner = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        _processing.value = true; _error.value = null
        viewModelScope.launch {
            try {
                val payment = if (create) RetrofitInstance.api.createPayment(booking) else RetrofitInstance.api.getPayment(booking)
                if (idDat == booking && owner == com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid) {
                    _payment.value = payment; _trangThaiThanhToan.value = payment.status
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (idDat == booking && owner == auth.currentUser?.uid) _error.value = "Chưa lấy được thông tin thanh toán. Kiểm tra đăng nhập, đơn đã xác nhận và thử lại."
            } finally { _processing.value = false }
        }
    }
}
