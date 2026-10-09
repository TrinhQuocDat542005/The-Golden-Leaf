package com.example.giaodien.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.giaodien.data.model.Notification
import com.example.giaodien.data.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class NotificationViewModel(private val repo: NotificationRepository,
    private val session: com.example.giaodien.data.network.AccountSession = com.example.giaodien.data.network.FirebaseAccountSession()) : ViewModel() {
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading
    private var polling: Job? = null
    private var owner: String? = null
    private val stop = session.observe {
        val uid = session.uid()
        if (uid != owner || polling == null) {
            owner = uid; polling?.cancel(); _notifications.value = emptyList(); _unreadCount.value = 0; _errorMessage.value = null; _loading.value = false
            if (uid != null) polling = viewModelScope.launch {
                while (isActive && session.uid() == uid) {
                    refreshFor(uid)
                    delay(15000)
                }
            }
        }
    }
    private suspend fun refreshFor(uid: String) {
        if (_loading.value || session.uid() != uid) return
        _loading.value = true
        try {
            val list = repo.getUserNotifications(null, null)
            val count = repo.unreadCount()
            if (session.uid() == uid) { _notifications.value = list.distinctBy { it.id }; _unreadCount.value = count; _errorMessage.value = null }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { if (session.uid() == uid) _errorMessage.value = "Chưa tải được thông báo. Kiểm tra kết nối và thử lại." }
        finally { if (session.uid() == uid) _loading.value = false }
    }
    fun retry() { session.uid()?.let { uid -> viewModelScope.launch { refreshFor(uid) } } }
    fun markRead(id: Long) {
        val uid = owner
        viewModelScope.launch {
            try {
                repo.markRead(id)
                val count = repo.unreadCount()
                if (session.uid() == uid) { _notifications.value = _notifications.value.map { if (it.id == id) it.copy(readFlag = true) else it }; _unreadCount.value = count }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (session.uid() == uid) _errorMessage.value = "Chưa cập nhật được thông báo." }
        }
    }
    override fun onCleared() { stop(); super.onCleared() }
}
