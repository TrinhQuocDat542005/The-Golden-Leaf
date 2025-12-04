//package com.example.giaodien.viewmodel
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.example.giaodien.data.model.BanSlot
//import com.example.giaodien.data.network.RetrofitInstance
//import com.example.giaodien.data.repository.BanSlotRepository
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.launch
//
//class BanSlotViewModel : ViewModel() {
//
//    private val _slots = MutableStateFlow<List<BanSlot>>(emptyList())
//    val slots: StateFlow<List<BanSlot>> = _slots
//
//    init {
//        viewModelScope.launch {
//            try {
//                val fetched = RetrofitInstance.api.getBanSlots() // endpoint trả về danh sách BanSlot
//                println("👉 API trả về ${fetched.size} slot")
//                fetched.forEach {
//                    println("Slot -> id=${it.id}, ngay=${it.ngay}, gio=${it.khungGio}")
//                }
//                _slots.value = fetched
//            } catch (e: Exception) {
//                println("❌ Lỗi parse JSON: ${e.message}")
//                e.printStackTrace()
//            }
//        }
//    }
//
//    fun getSlotById(id: Long) = _slots.value.find { it.id == id }
//
//}

package com.example.giaodien.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.giaodien.data.model.BanSlot
import com.example.giaodien.data.network.RetrofitInstance
import com.example.giaodien.data.repository.BanSlotRepository // Giữ lại nếu cần cho các hàm khác
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BanSlotViewModel : ViewModel() {

    private val _slots = MutableStateFlow<List<BanSlot>>(emptyList())
    val slots: StateFlow<List<BanSlot>> = _slots

    init {
        fetchBanSlots() // Gọi lần đầu khi ViewModel được tạo
    }

    /**
     * Tải lại danh sách BanSlot mới nhất từ API và cập nhật StateFlow.
     * Hàm này phải được gọi lại mỗi khi cần làm mới dữ liệu (ví dụ: khi quay lại màn hình).
     */
    fun fetchBanSlots() {
        viewModelScope.launch {
            try {
                // Giả định RetrofitInstance.api.getBanSlots() trả về danh sách BanSlot mới nhất
                val fetched = RetrofitInstance.api.getBanSlots()
                println("👉 API trả về ${fetched.size} slot mới")
                // ... (có thể giữ lại các lệnh debug khác nếu cần)
                _slots.value = fetched
            } catch (e: Exception) {
                println("❌ Lỗi tải slot: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun getSlotById(id: Long) = _slots.value.find { it.id == id }
}