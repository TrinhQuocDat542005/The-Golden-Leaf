// Ví dụ: com.example.giaodien.ui.screens/GioHangScreen.kt

package com.example.giaodien.ui.screens
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.giaodien.data.network.RetrofitInstance// <-- CẦN IMPORT CÁCH LẤY API SERVICE
import com.example.giaodien.viewmodel.GioHangViewModelFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.giaodien.data.model.ThucDon
import com.example.giaodien.navigation.Screen
import com.example.giaodien.viewmodel.GioHangItem
import com.example.giaodien.viewmodel.GioHangViewModel
import java.text.NumberFormat
import java.util.Locale

// Định dạng tiền tệ
val currencyFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.GERMAN) // Ví dụ: 1.000.000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GioHangScreen(
    navController: NavController,
    gioHangViewModel: GioHangViewModel
) {
    val gioHangList by gioHangViewModel.gioHangList.collectAsState()
    val tongTien by gioHangViewModel.tongTien.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Giỏ Hàng Của Bạn", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        containerColor = Color.Black // Nền cả màn hình là Đen
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (gioHangList.isEmpty()) {
                Text(
                    text = "Giỏ hàng trống.",
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(gioHangList) { item ->
                        GioHangItemCard(item = item, gioHangViewModel = gioHangViewModel)
                    }


                    item { Spacer(modifier = Modifier.height(10.dp)) }

                    // Phần Tổng Tiền
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "TỔNG CỘNG:",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${currencyFormat.format(tongTien)} VND",
                                style = MaterialTheme.typography.titleLarge,
                                color = DeepRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(70.dp)) }
                }
            }

            // Nút "Xác Nhận Đặt Món"
            Button(
                onClick = {
                    gioHangViewModel.xacNhanDatMon(
                        onSuccess = {
                            navController.navigate(Screen.HoaDon.route) {
                                popUpTo(Screen.GioHang.route) { inclusive = true }
                            }

                        },
                        onError = { msg ->
                            println("Lỗi xác nhận đặt món: $msg")
                        }
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeepRed,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .height(50.dp)
            ) {
                Text("Xác Nhận Đặt Món")
            }

        }
    }
}

@Composable
fun GioHangItemCard(item: GioHangItem, gioHangViewModel: GioHangViewModel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // Row chính: ảnh + tên + tăng giảm
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = rememberAsyncImagePainter(item.thucDon.anh),
                    contentDescription = item.thucDon.tenMon,
                    modifier = Modifier.size(60.dp),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Thông tin món
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.thucDon.tenMon,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        "${currencyFormat.format(item.thucDon.gia)} VND",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                // Tăng giảm số lượng + tổng tiền
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${currencyFormat.format(item.thucDon.gia * item.quantity)} VND",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BrightRed,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Nút giảm
                        Button(
                            onClick = { gioHangViewModel.giamSoLuong(item) },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepRed, contentColor = Color.White),
                            modifier = Modifier.size(36.dp),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("-", fontWeight = FontWeight.Bold)
                        }

                        Text(
                            item.quantity.toString(),
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .width(24.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        // Nút tăng
                        Button(
                            onClick = { gioHangViewModel.tangSoLuong(item) },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepRed, contentColor = Color.White),
                            modifier = Modifier.size(36.dp),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("+", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Thanh gỡ món
            // Thanh gỡ món
            Box(
                modifier = Modifier
                    .fillMaxWidth() // Box chiếm full width để căn giữa
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center // căn giữa Row bên trong
            ) {
                Row(
                    modifier = Modifier
                        .width(140.dp) // điều chỉnh độ dài thanh gỡ món
                        .height(40.dp)
                        .background(Color.DarkGray.copy(alpha = 0.7f), shape = RoundedCornerShape(6.dp))
                        .clickable { gioHangViewModel.xoaMon(item) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center // icon + text căn giữa
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Gỡ món", tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gỡ món", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
