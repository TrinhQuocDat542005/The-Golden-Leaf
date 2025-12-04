package com.example.giaodien.data.repository

import com.example.giaodien.data.model.BinhLuan
import com.example.giaodien.data.network.RetrofitInstance
import com.example.giaodien.data.network.model.BinhLuanRequest


class BinhLuanRepository {
    private val api = RetrofitInstance.api

    suspend fun getBinhLuan(thucDonId: Long) = api.getBinhLuan(thucDonId)

    suspend fun addBinhLuan(thucDonId: Long, noiDung: String, token: String) =
        api.addBinhLuan(BinhLuanRequest(thucDonId, noiDung), "Bearer $token")
}

