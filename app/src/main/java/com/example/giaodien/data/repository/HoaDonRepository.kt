package com.example.giaodien.data.repository

import com.example.giaodien.data.network.ApiService
import com.example.giaodien.data.model.HoaDonRequest
import com.example.giaodien.data.model.HoaDonResponse

class HoaDonRepository(private val apiService: ApiService) {

    suspend fun taoHoaDon(request: HoaDonRequest): HoaDonResponse {
        val response = apiService.createHoaDon(request)
        if (response.isSuccessful && response.body() != null) {
            return response.body()!!
        } else {
            throw Exception("Thanh toán thất bại: ${response.code()} - ${response.message()}")
        }
    }
}

