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

class ChiTietHoaDonViewModel(private val repository: TaiKhoanRepository,
    private val session: com.example.giaodien.data.network.AccountSession = com.example.giaodien.data.network.FirebaseAccountSession()) : ViewModel() {

    private val _state = MutableStateFlow<ChiTietHoaDonState>(ChiTietHoaDonState.Loading)
    val state: StateFlow<ChiTietHoaDonState> = _state
    private var owner = session.uid()
    private var generation = 0
    private var job: kotlinx.coroutines.Job? = null
    private val stop = session.observe {
        if (session.uid() != owner) {
            owner = session.uid(); generation++; job?.cancel()
            _state.value = ChiTietHoaDonState.Error("Tài khoản đã thay đổi. Vui lòng mở lại đơn.")
        }
    }
    override fun onCleared() { stop(); super.onCleared() }

    fun loadChiTietHoaDon(idDat: Long) {
        val uid = session.uid() ?: run { _state.value = ChiTietHoaDonState.Error("Vui lòng đăng nhập để xem đơn."); return }
        val request = ++generation
        job?.cancel()
        job = viewModelScope.launch {
            _state.value = ChiTietHoaDonState.Loading
            try {
                val data = repository.getChiTietHoaDon(idDat)
                if (request == generation && uid == session.uid()) _state.value = ChiTietHoaDonState.Success(data)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (request == generation && uid == session.uid()) _state.value = ChiTietHoaDonState.Error("Chưa tải được chi tiết đơn. Kiểm tra kết nối và thử lại.")
            }
        }
    }
}
