package com.example.giaodien.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.giaodien.data.repository.TaiKhoanRepository

// Lớp này dùng để khởi tạo TaiKhoanViewModel và truyền TaiKhoanRepository vào
// Việc tạo file này sẽ giải quyết lỗi 'Unresolved reference'
class TaiKhoanViewModelFactory(
    private val repository: TaiKhoanRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaiKhoanViewModel::class.java)) {
            return TaiKhoanViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}