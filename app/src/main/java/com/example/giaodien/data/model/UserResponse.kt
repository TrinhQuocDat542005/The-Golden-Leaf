    package com.example.giaodien.data.network.model

    import kotlinx.serialization.Serializable

    // Đây là mô hình trả về từ Spring Boot Backend
    @Serializable
    data class UserResponse(
        val uid: String,
        val email: String,
        val ten: String,
        val firebaseProvider: String
    )