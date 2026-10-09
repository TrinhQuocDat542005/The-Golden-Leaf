package com.example.giaodien.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.giaodien.data.model.LichSuDonDayDuDTO // Import DTO đúng
import com.example.giaodien.data.repository.TaiKhoanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Chú ý: Cần đổi package của TaiKhoanViewModel nếu chưa đúng
class TaiKhoanViewModel(
    private val repository: TaiKhoanRepository,
    private val session: com.example.giaodien.data.network.AccountSession = com.example.giaodien.data.network.FirebaseAccountSession()
) : ViewModel() {

    // Đã sửa DatBanFullDTO thành LichSuDonDayDuDTO
    private val _choXacNhan = MutableStateFlow<List<LichSuDonDayDuDTO>>(emptyList())
    val choXacNhan: StateFlow<List<LichSuDonDayDuDTO>> = _choXacNhan
    val isLoading = MutableStateFlow(true)
    val errorMessage = MutableStateFlow<String?>(null)
    private val _lichSuDonDat = MutableStateFlow<List<LichSuDonDayDuDTO>>(emptyList())
    val lichSuDonDat: StateFlow<List<LichSuDonDayDuDTO>> = _lichSuDonDat
    private var generation = 0
    private var loadJob: kotlinx.coroutines.Job? = null
    private val stop = session.observe {
        generation++; loadJob?.cancel(); isLoading.value = false
        _choXacNhan.value = emptyList(); _lichSuDonDat.value = emptyList(); errorMessage.value = null
        if (session.uid() != null) loadData()
    }
    override fun onCleared() { stop(); super.onCleared() }

    fun loadData() {
        val uid = session.uid() ?: return
        val request = ++generation
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            try {
                val pending = repository.getChoXacNhan()
                val history = repository.getLichSuDonDat()
                if (session.uid() == uid && request == generation) { _choXacNhan.value = pending; _lichSuDonDat.value = history }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (session.uid() == uid && request == generation) errorMessage.value = "Chưa tải được lịch sử. Kiểm tra kết nối và thử lại."
            } finally {
                if (session.uid() == uid && request == generation) isLoading.value = false
            }
        }
    }
    fun huyDonDat(idDat: Long) {
        val owner = session.uid() ?: return
        val request = generation
        viewModelScope.launch {
            try {

                repository.huyDonDat(idDat)

                // Hủy thành công, tải lại dữ liệu để cập nhật UI
                if (session.uid() == owner && request == generation) loadData()

            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (session.uid() == owner && request == generation) errorMessage.value = "Không thể hủy đơn ở trạng thái hiện tại. Vui lòng kiểm tra lại."
            }
        }
    }
}
