// ApiModule.kt
package com.example.giaodien.di

import com.example.giaodien.data.network.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides
    @Singleton
    fun provideApiService(): ApiService {
        // Lấy từ singleton đã có trong RetrofitInstance
        return com.example.giaodien.data.network.RetrofitInstance.api
    }
}
