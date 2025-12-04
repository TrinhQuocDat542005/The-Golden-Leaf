package com.example.giaodien.viewmodel

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.ViewModel
import com.example.giaodien.data.model.ThucDon
import com.example.giaodien.data.model.GioHangMonAn
import com.example.giaodien.data.network.ApiService // <-- CẦN IMPORT API SERVICE
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Định nghĩa một lớp dữ liệu để lưu thông tin món ăn và số lượng
data class GioHangItem(
    val thucDon: ThucDon,
    val quantity: Int = 1
)

class GioHangViewModel(
    // ⚠️ Thêm ApiService vào constructor để thực hiện gọi API
    private val apiService: ApiService
) : ViewModel() {

    // 1. BIẾN LƯU TRỮ ID ĐẶT BÀN (DÙNG ĐỂ LIÊN KẾT GIỎ HÀNG)
    private val _currentDatBanId = MutableStateFlow<Long?>(null)
    val currentDatBanId: StateFlow<Long?> = _currentDatBanId

    // HÀM ĐỂ LƯU ID ĐẶT BÀN TỪ NAVIGATION
    fun setDatBanId(id: Long) {
        _currentDatBanId.value = id
    }

    // Danh sách giỏ hàng (MutableStateFlow để theo dõi trạng thái)
    private val _gioHangList = MutableStateFlow<List<GioHangItem>>(emptyList())
    val gioHangList: StateFlow<List<GioHangItem>> = _gioHangList

    // Tính tổng số tiền
    val tongTien: StateFlow<Double> = _gioHangList.map { items ->
        items.sumOf { it.thucDon.gia * it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0)

    // HÀM THÊM MÓN VÀO GIỎ HÀNG
    fun addToCart(mon: ThucDon) {
        _gioHangList.update { currentList ->
            val existingItem = currentList.find { it.thucDon.idThucDon == mon.idThucDon }
            if (existingItem != null) {
                currentList.map {
                    if (it.thucDon.idThucDon == mon.idThucDon) {
                        it.copy(quantity = it.quantity + 1)
                    } else {
                        it
                    }
                } // ← return list mới
            } else {
                currentList + GioHangItem(mon, 1) // list mới với món thêm vào
            }
        }
    }
    fun tangSoLuong(item: GioHangItem) {
        _gioHangList.value = _gioHangList.value.map {
            if (it.thucDon.idThucDon == item.thucDon.idThucDon) it.copy(quantity = it.quantity + 1)
            else it
        }
    }

    fun giamSoLuong(item: GioHangItem) {
        _gioHangList.value = _gioHangList.value.map {
            if (it.thucDon.idThucDon == item.thucDon.idThucDon && it.quantity > 1)
                it.copy(quantity = it.quantity - 1)
            else it
        }
    }

    fun xoaMon(item: GioHangItem) {
        _gioHangList.value = _gioHangList.value.filter { it.thucDon.idThucDon != item.thucDon.idThucDon }
    }

    fun clearCart() {
        _gioHangList.value = emptyList()
    }

    // HÀM XỬ LÝ XÁC NHẬN ĐẶT MÓN (GỬI LÊN SERVER)
    fun xacNhanDatMon(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val datBanId = _currentDatBanId.value
        val gioHang = _gioHangList.value

        if (datBanId == null || gioHang.isEmpty()) {
            onError("Chưa có thông tin đặt bàn hoặc giỏ hàng trống.")
            return
        }

        // CHUYỂN ĐỔI: Mapping GioHangItem sang GioHangMonAn để gửi lên Server
        val danhSachGioHang = gioHang.map { item ->
            GioHangMonAn(
                idDat = datBanId,
                idThucDon = item.thucDon.idThucDon,
                tenMon = item.thucDon.tenMon,
                soLuong = item.quantity,
                giaMon = item.thucDon.gia
            )
        }

        viewModelScope.launch {
            try {
                // ✅ GỌI API THẬT SỰ VÀ KHÔNG CHỜ KẾT QUẢ TRẢ VỀ (Unit)
                apiService.postGioHang(danhSachGioHang)

                // Sau khi gửi thành công:
//                _gioHangList.value = emptyList()
//                _currentDatBanId.value = null
                onSuccess() // Kích hoạt điều hướng về Main
            } catch (e: Exception) {
                onError("Lỗi khi gửi đặt món: ${e.message ?: "Lỗi không xác định"}")
            }
        }
    }
}