package com.example.giaodien.data.repository

import com.example.giaodien.data.model.BanSlot
import com.example.giaodien.data.network.RetrofitInstance

class BanSlotRepository {

    suspend fun getBanSlots(): List<BanSlot> {
        return RetrofitInstance.api.getBanSlots()
    }

    suspend fun reserveBanSlot(ngay: String, khungGio: String, soLuong: Int): BanSlot {
        return RetrofitInstance.api.reserveBanSlot(ngay, khungGio, soLuong)
    }
}
