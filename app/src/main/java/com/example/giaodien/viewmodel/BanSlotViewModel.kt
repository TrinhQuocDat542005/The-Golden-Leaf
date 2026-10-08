package com.example.giaodien.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.giaodien.data.model.BanSlot
import com.example.giaodien.data.network.RetrofitInstance
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BanSlotViewModel(private val fetch: suspend () -> List<BanSlot> = { RetrofitInstance.api.getBanSlots() }) : ViewModel() {
    private val _slots = MutableStateFlow<List<BanSlot>>(emptyList())
    val slots: StateFlow<List<BanSlot>> = _slots
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    init { fetchBanSlots() }
    fun fetchBanSlots() {
        if (_loading.value) return
        _loading.value = true; _error.value = null
        viewModelScope.launch {
            try { _slots.value = fetch() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { _slots.value = emptyList(); _error.value = "Không tải được lịch bàn. Kiểm tra kết nối và thử lại." }
            finally { _loading.value = false }
        }
    }
    fun getSlotById(id: Long) = _slots.value.find { it.id == id }
}
