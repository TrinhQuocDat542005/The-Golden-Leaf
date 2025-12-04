package com.example.giaodien.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.giaodien.data.model.LichSuDonDayDuDTO
import com.example.giaodien.data.repository.TaiKhoanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ChiTietHoaDonState {
    object Loading : ChiTietHoaDonState()
    data class Success(val data: LichSuDonDayDuDTO) : ChiTietHoaDonState()
    data class Error(val message: String) : ChiTietHoaDonState()
}

class ChiTietHoaDonViewModel(private val repository: TaiKhoanRepository) : ViewModel() {

    private val _state = MutableStateFlow<ChiTietHoaDonState>(ChiTietHoaDonState.Loading)
    val state: StateFlow<ChiTietHoaDonState> = _state

    fun loadChiTietHoaDon(idDat: Long) {
        viewModelScope.launch {
            _state.value = ChiTietHoaDonState.Loading
            try {
                val data = repository.getChiTietHoaDon(idDat)
                _state.value = ChiTietHoaDonState.Success(data)
            } catch (e: Exception) {
                _state.value = ChiTietHoaDonState.Error(e.message ?: "Lỗi không xác định")
            }
        }
    }
}
