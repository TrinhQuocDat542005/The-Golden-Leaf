    package com.example.giaodien.data.network

    import com.example.giaodien.data.model.Notification
    import com.example.giaodien.data.model.*
    import com.example.giaodien.data.network.model.*
    import retrofit2.http.*
    import com.example.giaodien.data.model.LichSuDonDayDuDTO // Sử dụng DTO này
    interface ApiService {

        @GET("api/thucdon")
        suspend fun getThucDon(): List<ThucDon>
        @GET("api/thucdon/{id}")
        suspend fun getThucDonById(@Path("id") id: Long): ThucDon
        @GET("api/binhluan/{thucDonId}")
        suspend fun getBinhLuan(@Path("thucDonId") thucDonId: Long): List<BinhLuan>

        @POST("api/binhluan/add")
        suspend fun addBinhLuan(
            @Body request: BinhLuanRequest,
            @Header("Authorization") token: String
        ): BinhLuan

        @POST("api/auth/sync")
        suspend fun syncUser(@Body request: TokenRequest): UserResponse

        @POST("api/datban/save")
        suspend fun createDatBan(@Body datBan: DatBan): DatBan

        @GET("api/datban/latest")
        suspend fun getLatestDatBan(): DatBan

        @GET("api/ban-slot")
        suspend fun getBanSlots(): List<BanSlot>

        @POST("api/ban-slot/dat")
        suspend fun reserveBanSlot(
            @Query("ngay") ngay: String,
            @Query("khungGio") khungGio: String,
            @Query("soLuongKhach") soLuongKhach: Int
        ): BanSlot

        @POST("api/giohang/datmon")
        suspend fun postGioHang(
            @Body danhSachMon: List<GioHangMonAn>
        ): Unit

        // 🆕 API thanh toán hóa đơn
        @POST("api/hoadon/create")
        suspend fun createHoaDon(
            @Body request: HoaDonRequest
        ): retrofit2.Response<HoaDonResponse>
        @GET("api/notifications")
        suspend fun getNotifications(
            @Query("userId") userId: Long?,
            @Query("userEmail") userEmail: String?
        ): List<Notification>   // ✅ Chú ý dùng model Notification

        @POST("api/notifications/{id}/read")
        suspend fun markNotificationRead(@Path("id") id: Long)


        @GET("api/yeu-thich/list")
        suspend fun getFavorites(@Query("userId") userId: String): List<ThucDon>

        @POST("api/yeu-thich/add")
        suspend fun addFavorite(
            @Query("userId") userId: String,
            @Query("idThucDon") idThucDon: Long
        )

        @DELETE("api/yeu-thich/remove")
        suspend fun removeFavorite(
            @Query("userId") userId: String,
            @Query("idThucDon") idThucDon: Long
        )

        @GET("api/taikhoan/choXacNhan")
        suspend fun getChoXacNhan(): List<LichSuDonDayDuDTO>

        @GET("api/taikhoan/lichSuDonDat")
        suspend fun getLichSuDonDat(): List<LichSuDonDayDuDTO>

        @GET("api/dondat/{idDat}")
        suspend fun getChiTietDon(@Path("idDat") idDat: Long): LichSuDonDayDuDTO

        @DELETE("api/taikhoan/huyDon/{idDat}")
        suspend fun huyDonDat(@Path("idDat") idDat: Long)
    }
