//package com.example.giaodien.ui.viewmodel
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.example.giaodien.data.model.Notification
//import com.example.giaodien.data.repository.NotificationRepository
//import com.google.firebase.auth.FirebaseAuth
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.flow.*
//import kotlinx.coroutines.launch
//
//class NotificationViewModel(private val repo: NotificationRepository) : ViewModel() {
//
//    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
//    val notifications: StateFlow<List<Notification>> = _notifications
//
//    val unreadCount: StateFlow<Int> = _notifications
//        .map { list -> list.count { !it.readFlag } }
//        .stateIn(viewModelScope, SharingStarted.Lazily, 0)
//
//    init {
//        val currentUser = FirebaseAuth.getInstance().currentUser
//        startPolling(userEmail = currentUser?.email)
//    }
//
//
//    private fun startPolling(userId: Long? = null, userEmail: String? = null) {
//        viewModelScope.launch {
//            while (true) {
//                try {
//                    val list = repo.getUserNotifications(userId, userEmail)
//                    _notifications.value = list
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
//                delay(5000)
//            }
//        }
//    }
//
//    fun markRead(id: Long) {
//        viewModelScope.launch {
//            repo.markRead(id)
//            _notifications.value = _notifications.value.map {
//                if (it.id == id) it.copy(readFlag = true) else it
//            }
//        }
//    }
//}
package com.example.giaodien.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.giaodien.data.model.Notification
import com.example.giaodien.data.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NotificationViewModel(private val repo: NotificationRepository) : ViewModel() {

    // ⚡ Chỉ lưu notifications hợp lệ từ API
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications

    // ⚡ Lưu số lượng chưa đọc
    val unreadCount: StateFlow<Int> = _notifications
        .map { list -> list.count { !it.readFlag } }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    // ⚡ Lưu lỗi riêng, không push vào notifications
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        val currentUser = FirebaseAuth.getInstance().currentUser
        startPolling(userEmail = currentUser?.email)
    }

    private fun startPolling(userId: Long? = null, userEmail: String? = null) {
        viewModelScope.launch {
            while (true) {
                try {
                    // ⚡ Lấy danh sách notifications từ API
                    val list = repo.getUserNotifications(userId, userEmail)
                    // ⚡ Loại bỏ duplicate theo id
                    val distinctList = list.distinctBy { it.id }
                    _notifications.value = distinctList
                } catch (e: Exception) {
                    // ⚡ Chỉ lưu lỗi, không push vào notifications
                    _errorMessage.value = e.message
                }
                delay(5000)
            }
        }
    }

    fun markRead(id: Long) {
        viewModelScope.launch {
            try {
                repo.markRead(id)
                // Cập nhật trạng thái readFlag trên list local
                _notifications.value = _notifications.value.map {
                    if (it.id == id) it.copy(readFlag = true) else it
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }
}
