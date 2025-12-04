package com.example.giaodien.data.repository

import com.example.giaodien.data.network.ApiService
import com.example.giaodien.data.network.model.TokenRequest
import com.example.giaodien.data.network.model.UserResponse

/**
 * UserRepository chịu trách nhiệm xử lý logic dữ liệu người dùng,
 * bao gồm việc đồng bộ hóa thông tin người dùng với Backend.
 * Nó tách biệt logic gọi mạng khỏi ViewModel.
 */
class UserRepository(private val apiService: ApiService) {

    suspend fun synchronizeUser(idToken: String): UserResponse {
        val request = TokenRequest(token = idToken)
        return apiService.syncUser(request)
    }
}
