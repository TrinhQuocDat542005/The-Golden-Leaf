package com.example.giaodien.data.repository

import com.example.giaodien.data.model.BinhLuan
import com.example.giaodien.data.model.BinhLuanRequest
import com.example.giaodien.data.network.ApiService
import com.example.giaodien.data.network.RetrofitInstance

interface ReviewRepository {
    suspend fun list(id: Long): List<BinhLuan>
    suspend fun save(id: Long, content: String, rating: Int): BinhLuan
}
class BinhLuanRepository(private val api: ApiService = RetrofitInstance.api) : ReviewRepository {
    override suspend fun list(id: Long) = api.getBinhLuan(id)
    override suspend fun save(id: Long, content: String, rating: Int) =
        api.addBinhLuan(BinhLuanRequest(id, content, rating))
}

