package com.example.giaodien.data.network

import okhttp3.Interceptor
import okhttp3.Response

/** No refresh/replay for memory-only demo sessions. Network failures are not logout events. */
class SessionAuthInterceptor<T : Any>(
    private val current: () -> T?,
    private val token: (T) -> String,
    private val unauthorized: (T) -> Unit
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val snapshot = current()
        val request = if (snapshot == null) chain.request() else chain.request().newBuilder()
            .header("Authorization", "Bearer ${token(snapshot)}").build()
        val response = chain.proceed(request)
        if (response.code == 401 && snapshot != null && current() === snapshot) unauthorized(snapshot)
        return response
    }
}
