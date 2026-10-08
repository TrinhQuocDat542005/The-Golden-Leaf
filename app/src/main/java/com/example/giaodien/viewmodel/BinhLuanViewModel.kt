package com.example.giaodien.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.giaodien.data.model.BinhLuan
import com.example.giaodien.data.repository.BinhLuanRepository
import com.example.giaodien.data.repository.ReviewRepository
import com.example.giaodien.data.network.AccountSession
import com.example.giaodien.data.network.FirebaseAccountSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BinhLuanViewModel(
    private val repository: ReviewRepository = BinhLuanRepository(),
    private val session: AccountSession = FirebaseAccountSession()
) : ViewModel() {
    private val _binhLuanList = MutableStateFlow<List<BinhLuan>>(emptyList())
    val binhLuanList: StateFlow<List<BinhLuan>> = _binhLuanList
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading
    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private var generation = 0
    private var selectedId: Long? = null
    private var owner = session.uid()
    private var accountGeneration = 0
    private val unsubscribe = session.observe {
        if (owner != session.uid()) {
            owner = session.uid(); generation++; accountGeneration++; _binhLuanList.value = emptyList()
            _error.value = null; _loading.value = false; _sending.value = false
        }
    }
    override fun onCleared() { unsubscribe(); super.onCleared() }
    fun loadBinhLuan(id: Long) {
        if (selectedId != id) _binhLuanList.value = emptyList()
        selectedId = id
        val request = ++generation
        _loading.value = true; _error.value = null
        viewModelScope.launch {
            try { val result = repository.list(id); if (request == generation) _binhLuanList.value = result }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (request == generation) _error.value = "Không tải được đánh giá. Vui lòng thử lại." }
            finally { if (request == generation) _loading.value = false }
        }
    }
    fun addBinhLuan(id: Long, content: String, rating: Int, onSuccess: () -> Unit = {}) {
        if (_sending.value) return
        if (session.uid() == null) { _error.value = "Vui lòng đăng nhập để đánh giá."; return }
        if (content.isBlank() || content.length > 2000 || rating !in 1..5) { _error.value = "Nhập 1–2000 ký tự và điểm 1–5."; return }
        val request = generation; val uid = session.uid()
        val account = accountGeneration
        _sending.value = true; _error.value = null
        viewModelScope.launch {
            try {
                repository.save(id, content.trim(), rating)
                if (request == generation && uid == session.uid()) { onSuccess(); loadBinhLuan(id) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (request == generation && uid == session.uid()) _error.value = "Chưa gửi được đánh giá. Nội dung vẫn được giữ để thử lại." }
            finally { if (account == accountGeneration && uid == session.uid()) _sending.value = false }
        }
    }
}
