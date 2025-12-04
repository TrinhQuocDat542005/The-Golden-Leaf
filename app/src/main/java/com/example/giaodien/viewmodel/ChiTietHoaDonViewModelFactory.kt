package com.example.giaodien.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.giaodien.data.repository.TaiKhoanRepository


class ChiTietHoaDonViewModelFactory(
    private val repository: TaiKhoanRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChiTietHoaDonViewModel::class.java)) {
            return ChiTietHoaDonViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
