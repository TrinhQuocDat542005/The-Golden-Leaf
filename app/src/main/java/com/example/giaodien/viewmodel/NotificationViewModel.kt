package com.example.giaodien.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.giaodien.data.model.Notification
import com.example.giaodien.data.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class NotificationViewModel(private val repo: NotificationRepository) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    private var polling: Job? = null
    private var owner: String? = null
    private val listener = FirebaseAuth.AuthStateListener { firebase ->
        val uid = firebase.currentUser?.uid
        if (uid != owner || polling == null) {
            owner = uid; polling?.cancel(); _notifications.value = emptyList(); _unreadCount.value = 0; _errorMessage.value = null
            if (uid != null) polling = viewModelScope.launch {
                while (isActive && auth.currentUser?.uid == uid) {
                    try {
                        val list = repo.getUserNotifications(null, null)
                        val count = repo.unreadCount()
                        if (auth.currentUser?.uid == uid) { _notifications.value = list.distinctBy { it.id }; _unreadCount.value = count }
                        _errorMessage.value = null
                    } catch (e: CancellationException) { throw e }
                    catch (_: Exception) { _errorMessage.value = "Chưa tải được thông báo." }
                    delay(15000)
                }
            }
        }
    }
    init { auth.addAuthStateListener(listener) }
    fun markRead(id: Long) {
        val uid = owner
        viewModelScope.launch {
            try {
                repo.markRead(id)
                val count = repo.unreadCount()
                if (auth.currentUser?.uid == uid) { _notifications.value = _notifications.value.map { if (it.id == id) it.copy(readFlag = true) else it }; _unreadCount.value = count }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _errorMessage.value = "Chưa cập nhật được thông báo." }
        }
    }
    override fun onCleared() { auth.removeAuthStateListener(listener); super.onCleared() }
}
