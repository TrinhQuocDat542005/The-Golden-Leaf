package com.example.giaodien.data.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException

@Serializable
private data class ApiFailure(val message: String = "Yêu cầu không thành công")

private val errorJson = Json { ignoreUnknownKeys = true }

fun Throwable.userMessage(): String {
    if (this is HttpException) {
        val body = response()?.errorBody()?.string()
        return body?.let {
            runCatching { errorJson.decodeFromString<ApiFailure>(it).message }.getOrNull()
        } ?: "Yêu cầu không thành công (HTTP ${code()}). Vui lòng thử lại."
    }
    return "Không thể kết nối máy chủ. Vui lòng kiểm tra mạng và thử lại."
}
