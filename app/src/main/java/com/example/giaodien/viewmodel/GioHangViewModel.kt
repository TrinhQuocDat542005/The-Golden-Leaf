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
import androidx.lifecycle.SavedStateHandle
import com.example.giaodien.data.network.userMessage
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CancellationException

// Định nghĩa một lớp dữ liệu để lưu thông tin món ăn và số lượng
@Serializable
data class GioHangItem(
    val thucDon: ThucDon,
    val quantity: Int = 1
)

class GioHangViewModel(
    // ⚠️ Thêm ApiService vào constructor để thực hiện gọi API
    private val apiService: ApiService,
    private val savedState: SavedStateHandle
) : ViewModel() {

    // 1. BIẾN LƯU TRỮ ID ĐẶT BÀN (DÙNG ĐỂ LIÊN KẾT GIỎ HÀNG)
    private val _currentDatBanId = MutableStateFlow(savedState.get<Long>("bookingId"))
    val currentDatBanId: StateFlow<Long?> = _currentDatBanId
    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting
    private val _holdExpiresAt = MutableStateFlow(savedState.get<String>("holdExpiresAt"))
    val holdExpiresAt: StateFlow<String?> = _holdExpiresAt

    // HÀM ĐỂ LƯU ID ĐẶT BÀN TỪ NAVIGATION
    fun setDatBanId(id: Long, expiresAt: String? = null) {
        if (_currentDatBanId.value != id) clearCart()
        _currentDatBanId.value = id
        savedState["bookingId"] = id
        savedState["holdExpiresAt"] = expiresAt
        _holdExpiresAt.value = expiresAt
    }

    // Danh sách giỏ hàng (MutableStateFlow để theo dõi trạng thái)
    private val _gioHangList = MutableStateFlow<List<GioHangItem>>(
        savedState.get<String>("cart")?.let { runCatching { Json.decodeFromString<List<GioHangItem>>(it) }.getOrNull() }
            ?: emptyList()
    )
    val gioHangList: StateFlow<List<GioHangItem>> = _gioHangList

    private val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    private val accountListener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { firebase ->
        val uid = firebase.currentUser?.uid
        if (uid == null || savedState.get<String>("cartOwnerUid") != uid) {
            _gioHangList.value = emptyList(); _currentDatBanId.value = null; _holdExpiresAt.value = null
            savedState.remove<Long>("bookingId"); savedState.remove<String>("holdExpiresAt"); savedState.remove<String>("cart")
        }
        savedState["cartOwnerUid"] = uid
    }
    override fun onCleared() { auth.removeAuthStateListener(accountListener); super.onCleared() }

    init {
        auth.addAuthStateListener(accountListener)
        viewModelScope.launch {
            _gioHangList.collect { savedState["cart"] = Json.encodeToString(it) }
        }
    }

    // Tính tổng số tiền
    val tongTien: StateFlow<Double> = _gioHangList.map { items ->
        items.sumOf { it.thucDon.gia * it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0)

    // HÀM THÊM MÓN VÀO GIỎ HÀNG
    fun addToCart(mon: ThucDon) {
        if (_submitting.value) return
        _gioHangList.update { currentList ->
            val existingItem = currentList.find { it.thucDon.idThucDon == mon.idThucDon }
            if (existingItem != null) {
                currentList.map {
                    if (it.thucDon.idThucDon == mon.idThucDon) {
                        it.copy(quantity = minOf(100, it.quantity + 1))
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
        if (_submitting.value) return
        _gioHangList.value = _gioHangList.value.map {
            if (it.thucDon.idThucDon == item.thucDon.idThucDon) it.copy(quantity = minOf(100, it.quantity + 1))
            else it
        }
    }

    fun giamSoLuong(item: GioHangItem) {
        if (_submitting.value) return
        _gioHangList.value = _gioHangList.value.map {
            if (it.thucDon.idThucDon == item.thucDon.idThucDon && it.quantity > 1)
                it.copy(quantity = it.quantity - 1)
            else it
        }
    }

    fun xoaMon(item: GioHangItem) {
        if (_submitting.value) return
        _gioHangList.value = _gioHangList.value.filter { it.thucDon.idThucDon != item.thucDon.idThucDon }
    }

    fun clearCart() {
        if (_submitting.value) return
        _gioHangList.value = emptyList()
    }

    // HÀM XỬ LÝ XÁC NHẬN ĐẶT MÓN (GỬI LÊN SERVER)
    fun xacNhanDatMon(onSuccess: () -> Unit, onError: (String) -> Unit, allowEmpty: Boolean = false) {
        if (_submitting.value) return
        val datBanId = _currentDatBanId.value
        val gioHang = _gioHangList.value

        if (datBanId == null || (!allowEmpty && gioHang.isEmpty())) {
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

        _submitting.value = true
        val owner = auth.currentUser?.uid
        viewModelScope.launch {
            try {
                // Persist a full snapshot, then confirm; both calls can be safely retried.
                val serverItems = apiService.replaceGioHang(datBanId, danhSachGioHang)
                if (auth.currentUser?.uid != owner) return@launch
                _gioHangList.value = gioHang.map { item ->
                    val serverItem = serverItems.first { it.idThucDon == item.thucDon.idThucDon }
                    item.copy(thucDon = item.thucDon.copy(gia = serverItem.giaMon, tenMon = serverItem.tenMon))
                }
                apiService.confirmDatBan(datBanId)
                if (auth.currentUser?.uid != owner) return@launch
                _holdExpiresAt.value = null
                savedState["holdExpiresAt"] = null

                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError(e.userMessage())
            } finally {
                _submitting.value = false
            }
        }
    }

    fun cancelBooking(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val id = _currentDatBanId.value ?: return
        if (_submitting.value) return
        _submitting.value = true
        val owner = auth.currentUser?.uid
        viewModelScope.launch {
            try {
                apiService.cancelDatBan(id)
                if (auth.currentUser?.uid != owner) return@launch
                _gioHangList.value = emptyList()
                _currentDatBanId.value = null
                _holdExpiresAt.value = null
                savedState["bookingId"] = null
                savedState["holdExpiresAt"] = null
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError(e.userMessage())
            } finally {
                _submitting.value = false
            }
        }
    }
}
