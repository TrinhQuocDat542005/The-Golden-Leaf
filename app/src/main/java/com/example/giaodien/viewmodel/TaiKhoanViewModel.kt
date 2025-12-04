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
    private val _lichSuDonDat = MutableStateFlow<List<LichSuDonDayDuDTO>>(emptyList())
    val lichSuDonDat: StateFlow<List<LichSuDonDayDuDTO>> = _lichSuDonDat

    fun loadData() {
        viewModelScope.launch {
            isLoading.value = true
            try {
                _choXacNhan.value = repository.getChoXacNhan()
                _lichSuDonDat.value = repository.getLichSuDonDat()
            } catch (e: Exception) {
                println("Lỗi tải dữ liệu: ${e.message}")
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
                e.printStackTrace()
                // TODO: Xử lý lỗi (ví dụ: hiển thị Toast "Không thể hủy đơn hàng")
            }
        }
    }
}