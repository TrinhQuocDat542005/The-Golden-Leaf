package com.example.giaodien.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.example.giaodien.R
import com.example.giaodien.viewmodel.HoaDonViewModel
import kotlinx.coroutines.flow.collectLatest
@Composable
fun ThanhToanScreen(
    navController: NavController,
    method: String,
    viewModel: HoaDonViewModel
){
    val idDat = viewModel.idDat
    val tienBan = viewModel.tienBan
    val tienAn = viewModel.tienAn
    val tongTien = tienBan + tienAn
    val trangThai by viewModel.trangThaiThanhToan.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    val qrImage = when (method) {
        "MoMo" -> R.drawable.qr_momo
        "VNPay" -> R.drawable.qr_vnpay
        "Ngân hàng" -> R.drawable.qr_bank
        "Thẻ tín dụng" -> R.drawable.qr_credit
        else -> R.drawable.qr_default
    }

    LaunchedEffect(trangThai) {
        when (trangThai) {
            "success" -> {
                snackbarHostState.showSnackbar("Thanh toán thành công!")
                // Điều hướng về TrangChuScreen và xóa tất cả back stack
                navController.navigate("trang_chu") {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            }
            "error" -> {
                snackbarHostState.showSnackbar("Thanh toán thất bại. Vui lòng thử lại!")
            }
        }
    }


    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("ID Đặt bàn: #$idDat", style = MaterialTheme.typography.titleMedium)
            Text("Tiền bàn: ${String.format("%,.0f", tienBan)} VND")
            Text("Tiền món ăn: ${String.format("%,.0f", tienAn)} VND")
            Text("Tổng tiền: ${String.format("%,.0f", tongTien)} VND", style = MaterialTheme.typography.titleLarge, color = BrightRed)

            Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(8.dp)) {
                Image(painter = painterResource(id = qrImage), contentDescription = "QR Code", modifier = Modifier.size(220.dp))
            }

            Button(
                onClick = { viewModel.thanhToan(method) },
                colors = ButtonDefaults.buttonColors(containerColor = BrightRed)
            ) {
                Text("Xác nhận thanh toán", color = Color.White)
            }
        }
    }
}
