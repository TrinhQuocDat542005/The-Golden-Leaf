package com.example.giaodien.data.repository

import com.example.giaodien.data.model.ThucDon
import com.example.giaodien.data.model.menuImageUrl
import com.example.giaodien.BuildConfig
import com.example.giaodien.data.network.ApiService
import com.example.giaodien.data.network.RetrofitInstance

interface FavoriteRepository {
    suspend fun list(): List<ThucDon>
    suspend fun add(id: Long)
    suspend fun remove(id: Long)
}
class YeuThichRepository(private val api: ApiService = RetrofitInstance.api) : FavoriteRepository {
    override suspend fun list() = api.getFavorites().map { it.copy(anh = menuImageUrl(it.anh, BuildConfig.API_BASE_URL)) }
    override suspend fun add(id: Long) = api.addFavorite(id)
    override suspend fun remove(id: Long) = api.removeFavorite(id)
}
