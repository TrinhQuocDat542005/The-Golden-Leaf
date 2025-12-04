package com.example.giaodien.data.repository

import com.example.giaodien.data.model.ThucDon
import com.example.giaodien.data.network.RetrofitInstance

class YeuThichRepository {

    private val api = RetrofitInstance.api

    suspend fun getFavorites(userId: String): List<ThucDon> {
        return api.getFavorites(userId)
    }

    suspend fun addFavorite(userId: String, idThucDon: Long) {
        api.addFavorite(userId, idThucDon)
    }

    suspend fun removeFavorite(userId: String, idThucDon: Long) {
        api.removeFavorite(userId, idThucDon)
    }
}
