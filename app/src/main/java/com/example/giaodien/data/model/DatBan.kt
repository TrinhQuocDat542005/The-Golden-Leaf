
package com.example.giaodien.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DatBan(
    val idDat: Long? = null,
    val email: String,
    val ten: String,
    val ngay: String,
    val khungGio: String,
    val soLuong: Int,
    val ghiChu: String,
    val viTriBan: String   // thêm dòng này
)
