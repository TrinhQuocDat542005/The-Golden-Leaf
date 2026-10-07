package com.example.giaodien.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.giaodien.data.model.LichSuDonDayDuDTO // Import DTO đúng
import com.example.giaodien.data.repository.TaiKhoanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Chú ý: Cần đổi package của TaiKhoanViewModel nếu chưa đúng
class TaiKhoanViewModel(private val repository: TaiKhoanRepository) : ViewModel() {

    // Đã sửa DatBanFullDTO thành LichSuDonDayDuDTO
    private val _choXacNhan = MutableStateFlow<List<LichSuDonDayDuDTO>>(emptyList())
    val choXacNhan: StateFlow<List<LichSuDonDayDuDTO>> = _choXacNhan
    val isLoading = MutableStateFlow(true)
    val errorMessage = MutableStateFlow<String?>(null)
    private val _lichSuDonDat = MutableStateFlow<List<LichSuDonDayDuDTO>>(emptyList())
    val lichSuDonDat: StateFlow<List<LichSuDonDayDuDTO>> = _lichSuDonDat
    private val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    private val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener {
        _choXacNhan.value = emptyList(); _lichSuDonDat.value = emptyList(); errorMessage.value = null
        if (it.currentUser != null) loadData()
    }
    init { auth.addAuthStateListener(listener) }
    override fun onCleared() { auth.removeAuthStateListener(listener); super.onCleared() }

    fun loadData() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            try {
                val pending = repository.getChoXacNhan()
                val history = repository.getLichSuDonDat()
                if (auth.currentUser?.uid == uid) { _choXacNhan.value = pending; _lichSuDonDat.value = history }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                errorMessage.value = "Chưa tải được lịch sử. Kiểm tra kết nối và thử lại."
            } finally {
                isLoading.value = false
            }
        }
    }
    fun huyDonDat(idDat: Long) {
        viewModelScope.launch {
            try {

                repository.huyDonDat(idDat)

                // Hủy thành công, tải lại dữ liệu để cập nhật UI
                loadData()

            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                errorMessage.value = "Không thể hủy đơn ở trạng thái hiện tại. Vui lòng kiểm tra lại."
            }
        }
    }
}
