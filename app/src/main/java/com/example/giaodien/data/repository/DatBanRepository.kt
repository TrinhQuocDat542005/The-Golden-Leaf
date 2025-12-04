package com.example.giaodien.data.repository

import com.example.giaodien.data.model.DatBan
import com.example.giaodien.data.network.RetrofitInstance

class DatBanRepository {

    suspend fun datBan(datBan: DatBan): DatBan {
        return RetrofitInstance.api.createDatBan(datBan)
    }
    // ✅ THÊM HÀM MỚI: Lấy DatBan mới nhất từ Server
    suspend fun getLatestDatBan(): DatBan {
        // Hàm này tự động gửi email qua token nhờ AuthInterceptor
        return RetrofitInstance.api.getLatestDatBan()
    }
}
