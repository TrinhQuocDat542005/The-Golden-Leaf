package com.example.giaodien.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.giaodien.BuildConfig
import com.example.giaodien.data.model.ThucDon
import com.example.giaodien.data.model.menuImageUrl
import com.example.giaodien.data.repository.ThucDonRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ThucDonViewModel(private val fetch: suspend () -> List<ThucDon> = { ThucDonRepository().getAll() }) : ViewModel() {
    private val _thucDonList = MutableStateFlow<List<ThucDon>>(emptyList())
    val thucDonList: StateFlow<List<ThucDon>> = _thucDonList
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    fun loadThucDon() {
        if (_loading.value) return
        _loading.value = true; _error.value = null
        viewModelScope.launch {
            try { _thucDonList.value = fetch().map { it.copy(anh = menuImageUrl(it.anh, BuildConfig.API_BASE_URL)) } }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { _error.value = "Không tải được thực đơn. Vui lòng thử lại." }
            finally { _loading.value = false }
        }
    }
}
