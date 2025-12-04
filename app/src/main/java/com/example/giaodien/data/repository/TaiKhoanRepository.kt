package com.example.giaodien.data.repository

import com.example.giaodien.data.network.ApiService
import com.example.giaodien.data.model.LichSuDonDayDuDTO // Import DTO đúng

class TaiKhoanRepository(private val apiService: ApiService) {
    // Đã sửa DatBanFullDTO thành LichSuDonDayDuDTO
    suspend fun getChoXacNhan(): List<LichSuDonDayDuDTO> {
        return apiService.getChoXacNhan() // gọi backend trả về danh sách chờ xác nhận
    }

    // Đã sửa DatBanFullDTO thành LichSuDonDayDuDTO
    suspend fun getLichSuDonDat(): List<LichSuDonDayDuDTO> {
        return apiService.getLichSuDonDat() // gọi backend trả về lịch sử đơn
    }

    suspend fun getChiTietHoaDon(idDat: Long): LichSuDonDayDuDTO {
        return apiService.getChiTietDon(idDat)
    }

    suspend fun huyDonDat(idDat: Long) {
        apiService.huyDonDat(idDat)
    }
}