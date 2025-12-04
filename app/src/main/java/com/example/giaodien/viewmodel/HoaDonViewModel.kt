package com.example.giaodien.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.giaodien.data.model.HoaDonRequest
import com.example.giaodien.data.model.HoaDonResponse
import com.example.giaodien.data.repository.HoaDonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HoaDonViewModel(private val repository: HoaDonRepository) : ViewModel() {

    var idDat: Long = 0
    var tienBan: Double = 0.0
    var tienAn: Double = 0.0

    private val _trangThaiThanhToan = MutableStateFlow<String?>(null)
    val trangThaiThanhToan: StateFlow<String?> = _trangThaiThanhToan

    fun setThongTinThanhToan(idDat: Long, tienBan: Double, tienAn: Double) {
        this.idDat = idDat
        this.tienBan = tienBan
        this.tienAn = tienAn
    }

    fun thanhToan(method: String) {
        val tongTien = tienBan + tienAn
        viewModelScope.launch {
            try {
                val response: HoaDonResponse = repository.taoHoaDon(
                    HoaDonRequest(idDat, tienBan, tienAn, tongTien)
                )
                _trangThaiThanhToan.value = "success"
            } catch (e: Exception) {
                _trangThaiThanhToan.value = "error"
            }
        }
    }
}
