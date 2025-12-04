// File: com.example.giaodien.viewmodel/GioHangViewModelFactory.kt

package com.example.giaodien.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.giaodien.data.network.ApiService

class GioHangViewModelFactory(private val apiService: ApiService) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GioHangViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GioHangViewModel(apiService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}