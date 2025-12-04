package com.example.giaodien.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.giaodien.data.model.ThucDon
import com.example.giaodien.data.repository.YeuThichRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class YeuThichViewModel : ViewModel() {

    private val repository = YeuThichRepository()

    private val _favoriteList = MutableStateFlow<List<ThucDon>>(emptyList())
    val favoriteList: StateFlow<List<ThucDon>> = _favoriteList

    // Load danh sách yêu thích (cập nhật từ server)
    fun loadFavorites(userId: String) {
        viewModelScope.launch {
            try {
                _favoriteList.value = repository.getFavorites(userId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Toggle yêu thích
    fun toggleFavorite(userId: String, mon: ThucDon) {
        viewModelScope.launch {
            try {
                val exists = _favoriteList.value.any { it.idThucDon == mon.idThucDon }
                if (exists) {
                    repository.removeFavorite(userId, mon.idThucDon)
                } else {
                    repository.addFavorite(userId, mon.idThucDon)
                }
                loadFavorites(userId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Xóa món khỏi danh sách yêu thích
    fun removeFavorite(mon: ThucDon) {
        viewModelScope.launch {
            try {
                val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
                repository.removeFavorite(userId, mon.idThucDon)
                loadFavorites(userId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
