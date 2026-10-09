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

    // Không ghi body hoặc token; bản release tắt log mạng.
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        redactHeader("Authorization")
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
    }

    // Dùng token cache; authenticator bên dưới refresh một lần khi nhận 401.
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        if (BuildConfig.DEMO_MODE) {
            val state = DemoSession.state
            val request = if (state == null) originalRequest else originalRequest.newBuilder()
                .header("Authorization", "Bearer ${state.token}").build()
            val response = chain.proceed(request)
            if (response.code == 401 && DemoSession.state === state && state != null) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    if (DemoSession.state === state) DemoSession.signOut()
                }
            }
            return@Interceptor response
        }
        val user = FirebaseAuth.getInstance().currentUser

        // Retrofit chạy interceptor đồng bộ trên network thread.
        val token = try { runBlocking { user?.getIdToken(false)?.await()?.token } }
            catch (error: Exception) { throw java.io.IOException("Không lấy được token đăng nhập", error) }
        if (user?.uid != FirebaseAuth.getInstance().currentUser?.uid) throw java.io.IOException("Tài khoản đã thay đổi")

        val newRequest = if (token != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token") // Dùng header thay vì addHeader để ghi đè nếu đã có
                .build()
        } else originalRequest

        chain.proceed(newRequest)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .authenticator { _, response ->
            // One refresh only. OkHttp executes this on its network thread, never the Compose thread.
            if (BuildConfig.DEMO_MODE || response.priorResponse != null) null else {
                val user = FirebaseAuth.getInstance().currentUser
                val fresh = try { runBlocking { user?.getIdToken(true)?.await()?.token } } catch (_: Exception) { null }
                if (fresh == null || FirebaseAuth.getInstance().currentUser?.uid != user?.uid) null
                else response.request.newBuilder().header("Authorization", "Bearer $fresh").build()
            }
        }
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

