package com.example.giaodien.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TokenRequest(
    val idToken: String
)
