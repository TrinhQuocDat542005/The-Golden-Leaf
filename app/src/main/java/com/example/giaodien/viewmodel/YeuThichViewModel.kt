package com.example.giaodien.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.giaodien.data.model.ThucDon
import com.example.giaodien.data.repository.FavoriteRepository
import com.example.giaodien.data.repository.YeuThichRepository
import com.example.giaodien.data.network.AccountSession
import com.example.giaodien.data.network.FirebaseAccountSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class YeuThichViewModel(
    private val repository: FavoriteRepository = YeuThichRepository(),
    private val session: AccountSession = FirebaseAccountSession()
) : ViewModel() {
    private val _favoriteList = MutableStateFlow<List<ThucDon>>(emptyList())
    val favoriteList: StateFlow<List<ThucDon>> = _favoriteList
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private var generation = 0
    private var owner = session.uid()
    private val unsubscribe = session.observe {
        if (owner != session.uid()) { owner = session.uid(); generation++; _favoriteList.value = emptyList(); _error.value = null; _loading.value = false }
    }
    override fun onCleared() { unsubscribe(); super.onCleared() }
    // Compatibility argument only: identity is never sent as server authority.
    fun loadFavorites(userId: String) = run { repository.list() }
    fun toggleFavorite(userId: String, mon: ThucDon) = run {
        if (_favoriteList.value.any { it.idThucDon == mon.idThucDon }) repository.remove(mon.idThucDon) else repository.add(mon.idThucDon)
        repository.list()
    }
    fun removeFavorite(mon: ThucDon) = run { repository.remove(mon.idThucDon); repository.list() }
    private fun run(action: suspend () -> List<ThucDon>) {
        if (_loading.value) return
        if (session.uid() == null) { _error.value = "Vui lòng đăng nhập để dùng yêu thích."; return }
        val uid = session.uid(); val request = generation
        _loading.value = true; _error.value = null
        viewModelScope.launch {
            try { val result = action(); if (request == generation && uid == session.uid()) _favoriteList.value = result }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (request == generation && uid == session.uid()) _error.value = "Không cập nhật được yêu thích. Vui lòng thử lại." }
            finally { if (request == generation && uid == session.uid()) _loading.value = false }
        }
    }
}
