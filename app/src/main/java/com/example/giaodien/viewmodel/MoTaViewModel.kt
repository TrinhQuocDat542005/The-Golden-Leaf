package com.example.giaodien.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.giaodien.data.model.ThucDon
import com.example.giaodien.data.repository.ThucDonRepository
import kotlinx.coroutines.launch

class MoTaViewModel(
    private val repository: ThucDonRepository = ThucDonRepository()
) : ViewModel() {

    var monAn = mutableStateOf<ThucDon?>(null)
        private set

    var loading = mutableStateOf(false)
        private set

    var error = mutableStateOf<String?>(null)
        private set

    fun loadMonAn(id: Long) {
        viewModelScope.launch {
            try {
                loading.value = true
                monAn.value = repository.getThucDonById(id)
            } catch (e: Exception) {
                error.value = e.message
            } finally {
                loading.value = false
            }
        }
    }
}
