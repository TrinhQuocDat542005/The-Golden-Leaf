package com.example.giaodien.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.giaodien.ui.viewmodel.NotificationViewModel
import com.example.giaodien.data.repository.NotificationRepository

class NotificationViewModelFactory(private val repo: NotificationRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotificationViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
