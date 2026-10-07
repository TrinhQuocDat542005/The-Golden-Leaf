package com.example.giaodien.data.network

import com.example.giaodien.BuildConfig
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

// Firebase Auth
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor

object RetrofitInstance {

    private val BASE_URL = BuildConfig.API_BASE_URL

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    // Ghi log toàn bộ request / response
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // ✅ Interceptor lấy Firebase ID Token và gắn vào Header Authorization
    // Trong RetrofitInstance
    // ✅ INTERCEPTOR ĐÃ SỬA: Bắt buộc làm mới token (true) và sử dụng runBlocking an toàn hơn
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val user = FirebaseAuth.getInstance().currentUser

        // ⚠️ Lưu ý: Việc sử dụng runBlocking trong Interceptor vẫn là một giải pháp tạm thời,
        // nhưng cần thiết vì Interceptor không phải là hàm suspend.
        // Đặt nó ở đây giúp code đơn giản hơn.
        val token = runBlocking {
            // Đặt thành TRUE để luôn nhận token MỚI NHẤT
            user?.getIdToken(true)?.await()?.token
        }

        val newRequest = if (token != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token") // Dùng header thay vì addHeader để ghi đè nếu đã có
                .build()
        } else originalRequest

        chain.proceed(newRequest)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ApiService::class.java)
    }
}

