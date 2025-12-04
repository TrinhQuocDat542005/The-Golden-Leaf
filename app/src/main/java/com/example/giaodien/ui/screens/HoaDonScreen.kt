package com.example.giaodien.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.giaodien.viewmodel.DatBanViewModel
import kotlinx.coroutines.launch
import com.example.giaodien.navigation.Screen
import com.example.giaodien.viewmodel.GioHangViewModel
import com.example.giaodien.viewmodel.GioHangItem
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.window.Dialog
import com.example.giaodien.viewmodel.HoaDonViewModel

// Định nghĩa màu Đỏ Đô (DeepRed) - Giả định nếu chưa có import
val BackgroundDark = Color(0xFF121212) // Nền chính
val CardBackground = Color(0xFF1E1E1E) // Nền Card/Form
val TextPrimary = Color.White
val TextSecondary = Color(Color.White.value).copy(alpha = 0.7f)
val BrightRed = Color(0xFFD32F2F) // MÀU ĐỎ TƯƠI MỚI!


// 1. Hằng số Phí đặt bàn
const val PHI_DAT_BAN = 200000

@Composable
fun HoaDonScreen(
    navController: NavController,
    viewModel: DatBanViewModel = hiltViewModel(),
    gioHangViewModel: GioHangViewModel,
    hoaDonViewModel: HoaDonViewModel // <-- bắt buộc phải truyền từ AppNavGraph

) {
    val latestDatBan by viewModel.latestDatBan.collectAsState()
    val gioHangList by gioHangViewModel.gioHangList.collectAsState()
    val tongTienMonAn = remember(gioHangList) {
        gioHangList.sumOf { it.thucDon.gia * it.quantity }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // ✅ Trạng thái hiển thị modal thanh toán
    val showPaymentDialog = remember { mutableStateOf(false) }
    val selectedMethod = remember { mutableStateOf<String?>(null) }
    val tongTienThanhToan = (PHI_DAT_BAN + tongTienMonAn).toFloat()

    LaunchedEffect(Unit) {
        viewModel.fetchLatestDatBan { errorMessage ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = errorMessage,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundDark
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ================== NỘI DUNG CHÍNH ==================
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Quay lại",
                                tint = TextPrimary
                            )
                        }
                        Text(
                            "Chi Tiết Hóa Đơn",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(48.dp))
                    }
                }

                item {
                    when (val datBan = latestDatBan) {
                        null -> Box(
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = BrightRed)
                                Text(
                                    "Đang tìm kiếm đơn đặt bàn mới nhất...",
                                    modifier = Modifier.padding(top = 16.dp),
                                    color = TextSecondary
                                )
                            }
                        }

                        else -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                ThongTinDatBanCard(datBan)
                                MonAnDaChonCard(gioHangList)
                                TongKetCard(tongTienMonAn)
                                ThankYouMessage()
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }

            // ================== NÚT THANH TOÁN ==================
            Button(
                onClick = { showPaymentDialog.value = true },
                colors = ButtonDefaults.buttonColors(containerColor = BrightRed, contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .height(50.dp)
            ) {
                Text("Chọn Phương Thức Thanh Toán", fontWeight = FontWeight.Bold)
            }

            // ================== POPUP THANH TOÁN ==================
            if (showPaymentDialog.value) {
                Dialog(onDismissRequest = { showPaymentDialog.value = false }) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)) // Làm mờ nền
                    ) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp)
                                .fillMaxWidth(0.9f),
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            elevation = CardDefaults.cardElevation(8.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Chọn phương thức",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(Modifier.height(16.dp))

                                // Radio buttons chọn phương thức
                                val methods = listOf("MoMo", "VNPay", "Ngân hàng", "Thẻ tín dụng")
                                methods.forEach { method ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedMethod.value = method }
                                            .padding(vertical = 6.dp)
                                    ) {
                                        RadioButton(
                                            selected = selectedMethod.value == method,
                                            onClick = { selectedMethod.value = method },
                                            colors = RadioButtonDefaults.colors(selectedColor = BrightRed)
                                        )
                                        Text(
                                            text = method,
                                            color = TextPrimary,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(20.dp))

                                // Nút xác nhận
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    OutlinedButton(
                                        onClick = { showPaymentDialog.value = false },
                                        border = ButtonDefaults.outlinedButtonBorder,
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                                    ) {
                                        Text("Hủy")
                                    }

                                    Button(
                                        onClick = {
                                            val method = selectedMethod.value
                                            if (method != null) {
                                                showPaymentDialog.value = false

                                                // Set thông tin thanh toán
                                                // Set thông tin thanh toán
                                                hoaDonViewModel.setThongTinThanhToan(
                                                    idDat = latestDatBan?.idDat ?: 0L,
                                                    tienBan = PHI_DAT_BAN.toDouble(),
                                                    tienAn = tongTienMonAn
                                                )

// Chuyển sang trang Thanh toán
                                                navController.navigate(
                                                    Screen.ThanhToan.createRoute(
                                                        method = method,
                                                        idDat = latestDatBan?.idDat ?: 0L,
                                                        tienBan = PHI_DAT_BAN.toDouble(),
                                                        tienAn = tongTienMonAn
                                                    )
                                                )

                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BrightRed),
                                        enabled = selectedMethod.value != null
                                    ) {
                                        Text("Xác nhận")
                                    }


                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// ********** CÁC COMPOSABLE PHỤ (TÁCH FORM) **********
// =========================================================================

// Composable cho Form Thông tin Đặt bàn (Bao gồm Phí đặt bàn)
@Composable
fun ThongTinDatBanCard(datBan: com.example.giaodien.data.model.DatBan) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "📝 Chi tiết Đặt bàn",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Divider(Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.3f))

            ThongTinItem("ID Đặt Bàn:", "#${datBan.idDat}", TextPrimary, TextSecondary)
            ThongTinItem("Tên Khách:", datBan.ten, TextPrimary, TextSecondary)
            ThongTinItem("Ngày:", datBan.ngay, TextPrimary, TextSecondary)
            ThongTinItem("Khung Giờ:", datBan.khungGio, TextPrimary, TextSecondary)
            ThongTinItem("Vị Trí Bàn:", datBan.viTriBan, TextPrimary, TextSecondary)
            ThongTinItem("Số Lượng:", "${datBan.soLuong} người", TextPrimary, TextSecondary)
            if (datBan.ghiChu.isNotBlank()) {
                ThongTinItem("Ghi Chú:", datBan.ghiChu, TextPrimary, TextSecondary)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color.Gray.copy(alpha = 0.3f))

            // PHÍ ĐẶT BÀN CỐ ĐỊNH
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Phí đặt bàn:", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                Text("${String.format("%,d", PHI_DAT_BAN)} VND",
                    style = MaterialTheme.typography.bodyLarge,
                    color = BrightRed,
                    fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// Composable cho Form Món ăn đã chọn
@Composable
fun MonAnDaChonCard(gioHangList: List<GioHangItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "🍲 Món ăn đã chọn",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Divider(Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.3f))

            if (gioHangList.isEmpty()) {
                Text("Chưa có món ăn nào được chọn.", color = TextSecondary)
            } else {
                gioHangList.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Tên món và số lượng (căn trái)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(item.thucDon.tenMon, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("x${item.quantity}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }

                        // Tổng tiền cho món đó (căn phải)
                        val tongTienMon = item.thucDon.gia * item.quantity
                        Text("${String.format("%,.0f", tongTienMon)} VND", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    }
                }
            }
        }
    }
}

// Composable cho Form Tổng kết cuối cùng
@Composable
fun TongKetCard(tongTienMonAn: Double) {
    val tongTien = PHI_DAT_BAN + tongTienMonAn

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "💰 Tổng kết đơn hàng",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Divider(Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.3f))

            // Chi tiết tổng tiền món ăn
            ThongTinItem("Tổng tiền Món ăn:", "${String.format("%,.0f", tongTienMonAn)} VND", TextPrimary, BrightRed)

            // Chi tiết phí đặt bàn (để dễ đối chiếu)
            ThongTinItem("Phí đặt bàn:", "${String.format("%,d", PHI_DAT_BAN)} VND", TextPrimary, BrightRed)

            Spacer(modifier = Modifier.height(12.dp))
            Divider(thickness = 2.dp, color = Color.Gray.copy(alpha = 0.5f))

            // TỔNG CỘNG CUỐI CÙNG (Căn phải)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("THÀNH TIỀN:", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    "${String.format("%,.0f", tongTien)} VND",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = BrightRed // Màu Đỏ Đô nổi bật
                )
            }
            Divider(thickness = 2.dp, color = Color.Gray.copy(alpha = 0.5f))

        }
    }
}

// Composable Item chung
@Composable
fun ThongTinItem(
    label: String,
    value: String,
    labelColor: Color = TextSecondary,
    valueColor: Color = TextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = labelColor)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}
@Composable
fun ThankYouMessage() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        colors = CardDefaults.cardColors(containerColor = DeepRed.copy(alpha = 0.1f)), // Nền nhẹ nhàng
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Icon
            Icon(
                Icons.Default.Favorite, // Dùng icon trái tim hoặc bạn có thể dùng Star
                contentDescription = "Cảm ơn",
                tint = BrightRed, // Màu đỏ tươi cho icon
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))

            // Text Cảm ơn
            Text(
                text = "The Golden Leaf cảm ơn quý khách!",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.ExtraBold // Phông chữ đậm
            )
        }
    }
}