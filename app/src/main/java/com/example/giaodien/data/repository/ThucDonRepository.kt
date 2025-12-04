package com.example.giaodien.data.repository

import com.example.giaodien.data.model.ThucDon
import com.example.giaodien.data.network.ApiService
import com.example.giaodien.data.network.RetrofitInstance

class ThucDonRepository {
    private val api = RetrofitInstance.api
    suspend fun getAll(): List<ThucDon> = api.getThucDon()
    suspend fun getThucDonById(id: Long): ThucDon {
        return api.getThucDonById(id)
    }

}

