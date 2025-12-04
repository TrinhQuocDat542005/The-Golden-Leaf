// File: com.example.giaodien.data.network.model.TokenRequest.kt
package com.example.giaodien.data.network.model

import kotlinx.serialization.Serializable

@Serializable
data class TokenRequest(
    val token: String // ✅ đổi từ idToken → token
)
