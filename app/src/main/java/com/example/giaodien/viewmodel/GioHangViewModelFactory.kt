// File: com.example.giaodien.viewmodel/GioHangViewModelFactory.kt

package com.example.giaodien.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.giaodien.data.network.ApiService
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.createSavedStateHandle

class GioHangViewModelFactory(private val apiService: ApiService) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(GioHangViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GioHangViewModel(apiService, extras.createSavedStateHandle()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
