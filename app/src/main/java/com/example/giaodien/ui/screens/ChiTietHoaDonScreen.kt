package com.example.giaodien.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.giaodien.data.model.LichSuDonDayDuDTO
import com.example.giaodien.data.model.GioHangMonAn
import com.example.giaodien.viewmodel.ChiTietHoaDonState
import com.example.giaodien.viewmodel.ChiTietHoaDonViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChiTietHoaDonScreen(
    navController: NavHostController,
    idDat: Long,
    viewModel: ChiTietHoaDonViewModel
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(idDat) { viewModel.loadChiTietHoaDon(idDat) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết hóa đơn") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (state) {
            is ChiTietHoaDonState.Loading -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            is ChiTietHoaDonState.Error -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { Text("Lỗi: ${(state as ChiTietHoaDonState.Error).message}") }

            is ChiTietHoaDonState.Success -> {
                val d = (state as ChiTietHoaDonState.Success).data
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // --- Thông tin hóa đơn ---
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text("Thông tin đơn hàng #${d.idDat}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Spacer(Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                InfoDisplayRow("Vị trí", d.viTriBan)
                                Divider()
                                InfoDisplayRow("Tên khách", d.ten)
                                Divider()
                                InfoDisplayRow("Email", d.email)
                                Divider()
                                InfoDisplayRow("Ngày đặt", d.ngay)
                                Divider()
                                InfoDisplayRow("Khung giờ", d.khungGio)
                                Divider()
                                InfoDisplayRow("Số lượng", d.soLuong.toString())
                                Divider()
                                InfoDisplayRow("Ghi chú", d.ghiChu ?: "Không có")
                            }
                        }
                    }

                    item {
                        Divider()
                    }

                    // --- Chi tiết món ---
                    item {
                        Text("Chi tiết món đã gọi", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Spacer(Modifier.height(8.dp))
                    }

                    items(d.danhSachMon) { mon ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(mon.tenMon, fontWeight = FontWeight.SemiBold)
                                    // Text("Giá: ${formatMoney(mon.giaMon)}", fontSize = 12.sp, color = Color.Gray)
                                }
                                Text("x${mon.soLuong}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    item {
                        Divider()
                    }

                    // --- Tổng tiền ---
                    item {
                        Text("Tổng kết", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Spacer(Modifier.height(8.dp))
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                TotalDisplayRow("Tiền bàn (Phí dịch vụ)", formatMoney(d.tienBan))
                                TotalDisplayRow("Tiền ăn (Tổng món)", formatMoney(d.tienAn))
                                Divider(Modifier.padding(vertical = 4.dp))
                                TotalDisplayRow("TỔNG TIỀN", formatMoney(d.tongTien), isTotal = true)
                            }
                        }
                        Spacer(Modifier.height(40.dp))
                    }
                }
            }
        }
    }
}

/**
 * Hàm hỗ trợ hiển thị thông tin chi tiết một cách đẹp mắt (Label/Value).
 */
@Composable
fun InfoDisplayRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = FontWeight.Normal, color = Color.Gray)
        Text(value, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * Hàm hỗ trợ hiển thị dòng tổng kết (có thể in đậm tổng).
 */
@Composable
fun TotalDisplayRow(label: String, value: String, isTotal: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontWeight = if (isTotal) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (isTotal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        Text(
            value,
            fontWeight = if (isTotal) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (isTotal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

fun formatMoney(d: Double?): String = d?.let { "%,.0f VNĐ".format(it) } ?: "—"