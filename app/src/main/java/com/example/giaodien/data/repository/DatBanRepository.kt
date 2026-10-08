package com.example.giaodien.data.repository

import com.example.giaodien.data.model.DatBan
import com.example.giaodien.data.network.RetrofitInstance

open class DatBanRepository(private val api: com.example.giaodien.data.network.ApiService = RetrofitInstance.api) {

    open suspend fun datBan(datBan: DatBan, key: String): DatBan {
        return api.createDatBan(datBan, key)
    }
    open suspend fun getDatBan(id: Long): DatBan = api.getDatBan(id)
    // ✅ THÊM HÀM MỚI: Lấy DatBan mới nhất từ Server
    suspend fun getLatestDatBan(): DatBan {
        // Hàm này tự động gửi email qua token nhờ AuthInterceptor
        return api.getLatestDatBan()
    }
}
