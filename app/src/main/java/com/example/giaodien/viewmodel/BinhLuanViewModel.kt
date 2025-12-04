package com.example.giaodien.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.giaodien.data.model.BinhLuan
import com.example.giaodien.data.repository.BinhLuanRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class BinhLuanViewModel : ViewModel() {

    private val repository = BinhLuanRepository()

    private val _binhLuanList = MutableStateFlow<List<BinhLuan>>(emptyList())
    val binhLuanList: StateFlow<List<BinhLuan>> = _binhLuanList

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    fun loadBinhLuan(thucDonId: Long) {
        viewModelScope.launch {
            _loading.value = true
            try {
                _binhLuanList.value = repository.getBinhLuan(thucDonId)
            } finally {
                _loading.value = false
            }
        }
    }

    fun addBinhLuan(thucDonId: Long, noiDung: String) {
        viewModelScope.launch {
            val user = FirebaseAuth.getInstance().currentUser
            val token = user?.getIdToken(false)?.await()?.token
            if (!noiDung.isBlank() && token != null) {
                repository.addBinhLuan(thucDonId, noiDung, token)
                loadBinhLuan(thucDonId) // reload bình luận
            }
        }
    }

}
