package com.example.giaodien.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import com.example.giaodien.data.network.userMessage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CancellationException
import java.util.UUID
import androidx.lifecycle.viewModelScope
import com.example.giaodien.data.model.DatBan
import com.example.giaodien.data.repository.DatBanRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DatBanViewModel @JvmOverloads constructor(
    private val savedState: SavedStateHandle,
    private val repository: DatBanRepository = DatBanRepository(),
    private val session: com.example.giaodien.data.network.AccountSession = com.example.giaodien.data.network.FirebaseAccountSession()
) : ViewModel() {
    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Dữ liệu DatBan tạm thời
    private val _datBan = MutableStateFlow<DatBan?>(null)
    val datBan: StateFlow<DatBan?> get() = _datBan
    // Invoice screens load the exact booking ID belonging to the current flow.
    private val _latestDatBan = MutableStateFlow<DatBan?>(null)
    val latestDatBan: StateFlow<DatBan?> get() = _latestDatBan
    // Cho phép truy cập trực tiếp DatBan hiện tại
    val currentDatBan: DatBan?
        get() = _datBan.value

    private var accountGeneration = 0
    private val unsubscribe = session.observe {
        val uid = session.uid()
        if (uid == null || savedState.get<String>("bookingOwnerUid") != uid) {
            accountGeneration++
            savedState.remove<String>("bookingRequest"); savedState.remove<String>("bookingKey")
            savedState.remove<Long>("currentBookingId")
            _datBan.value = null; _latestDatBan.value = null; _error.value = null
        }
        savedState["bookingOwnerUid"] = uid
    }
    override fun onCleared() { unsubscribe(); super.onCleared() }

    fun setDatBan(datBan: DatBan) {
        _datBan.value = datBan
    }

    fun datBan(
        datBan: DatBan,
        intentId: String,
        onSuccess: (DatBan) -> Unit,
        onError: (String) -> Unit
    ) {
        if (_submitting.value) return
        _submitting.value = true
        _error.value = null
        val fingerprint = intentId + Json.encodeToString(datBan)
        if (savedState.get<String>("bookingRequest") != fingerprint) {
            savedState["bookingRequest"] = fingerprint
            savedState["bookingKey"] = UUID.randomUUID().toString()
        }
        val key = checkNotNull(savedState.get<String>("bookingKey"))
        val owner = session.uid()
        val generation = accountGeneration
        viewModelScope.launch {
            try {
                val createdDatBan = repository.datBan(datBan, key)
                if (session.uid() != owner || generation != accountGeneration) return@launch
                if (createdDatBan.status in setOf("CANCELLED", "EXPIRED")) {
                    savedState.remove<String>("bookingRequest")
                    savedState.remove<String>("bookingKey")
                    val message = "Lượt giữ chỗ đã hết hạn hoặc đã hủy. Vui lòng đặt lại."
                    _error.value = message
                    onError(message)
                    return@launch
                }
                savedState["currentBookingId"] = createdDatBan.idDat

                _datBan.value = createdDatBan // Cập nhật DatBan đã có ID
                onSuccess(createdDatBan)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (session.uid() != owner || generation != accountGeneration) return@launch
                val message = e.userMessage()
                _error.value = message
                onError(message)
            } finally {
                _submitting.value = false
            }
        }
    }
    // Never use the global latest booking to associate a customer's cart/invoice.
    fun fetchCurrentDatBan(id: Long?, onError: (String) -> Unit) {
        val owner = session.uid()
        val generation = accountGeneration
        viewModelScope.launch {
            try {
                val bookingId = id ?: savedState.get<Long>("currentBookingId")
                if (bookingId == null) {
                    _latestDatBan.value = null
                    onError("Chưa có thông tin đơn đặt bàn hiện tại.")
                    return@launch
                }
                val result = repository.getDatBan(bookingId)
                if (session.uid() != owner || generation != accountGeneration) return@launch
                _latestDatBan.value = result // Cập nhật state
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (session.uid() != owner || generation != accountGeneration) return@launch
                // Xử lý lỗi, ví dụ: không tìm thấy bản ghi nào hoặc lỗi mạng/server
                _latestDatBan.value = null
                onError(e.userMessage())
            }
        }
    }
    fun getUserEmail(): String {
        return FirebaseAuth.getInstance().currentUser?.email ?: ""
    }
}
