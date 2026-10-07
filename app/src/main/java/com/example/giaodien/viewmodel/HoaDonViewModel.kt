package com.example.giaodien.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.giaodien.data.model.HoaDonRequest
import com.example.giaodien.data.model.HoaDonResponse
import com.example.giaodien.data.repository.HoaDonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import java.math.BigDecimal
import java.math.RoundingMode

class HoaDonViewModel(private val repository: HoaDonRepository) : ViewModel() {

    var idDat: Long = 0
    var tienBan: Double = 0.0
    var tienAn: Double = 0.0

    private val _trangThaiThanhToan = MutableStateFlow<String?>(null)
    val trangThaiThanhToan: StateFlow<String?> = _trangThaiThanhToan
    private val _processing = MutableStateFlow(false)
    val processing: StateFlow<Boolean> = _processing

    fun setThongTinThanhToan(idDat: Long, tienBan: Double, tienAn: Double) {
        if (this.idDat != idDat) _trangThaiThanhToan.value = null
        this.idDat = idDat
        this.tienBan = tienBan
        this.tienAn = tienAn
    }

    fun thanhToan(method: String) {
        if (_processing.value) return
        _processing.value = true
        _trangThaiThanhToan.value = null
        val fee = BigDecimal.valueOf(tienBan).setScale(2, RoundingMode.HALF_UP)
        val food = BigDecimal.valueOf(tienAn).setScale(2, RoundingMode.HALF_UP)
        val total = fee.add(food)
        viewModelScope.launch {
            try {
                val response: HoaDonResponse = repository.taoHoaDon(
                    HoaDonRequest(idDat, fee.toDouble(), food.toDouble(), total.toDouble())
                )
                _trangThaiThanhToan.value = "success"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _trangThaiThanhToan.value = "error"
            } finally {
                _processing.value = false
            }
        }
    }
}
