package com.example.giaodien.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.giaodien.R // Đảm bảo bạn có một drawable cho biểu tượng đĩa/dao/thìa và biểu tượng người
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.BasicTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NhapSoLuongScreen(
    navController: NavController,
    ngayChon: String,
    khungGioChon: String,
    viTriBan: String,
    banConLai: Int,
    submitting: Boolean = false,
    error: String? = null,
    onDatBan: (soLuong: Int, ghiChu: String) -> Unit
) {
    var soLuong by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(1) }
    var ghiChu by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Số lượng",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Xử lý thông báo */ }) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Thông báo",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFE55C50) // Màu đỏ cam
                )
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .background(Color(0xFFF0F0F0)), // Màu nền tổng thể nhạt
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Thẻ chứa nội dung chính
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF282828)), // Nền tối cho Card
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // "Còn trống: 84" và biểu tượng người
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Còn trống: $banConLai bàn",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Icon(
                                painter = painterResource(id = R.drawable.ic_person), // Thay bằng drawable của bạn
                                contentDescription = "Số người",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // Biểu tượng đĩa và dao nĩa
                        coil.compose.AsyncImage(
                            model = R.drawable.logo_sl,
                            contentDescription = "Biểu tượng đĩa",
                            modifier = Modifier.size(200.dp)
                        )

                        Spacer(Modifier.height(32.dp))

                        // Bộ điều chỉnh số lượng
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Nút giảm
                            Button(
                                onClick = { if (soLuong > 1) soLuong-- },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                shape = CircleShape,
                                modifier = Modifier.size(56.dp).testTag("guests-minus")
                            ) {
                                Text("-", fontSize = 28.sp, color = Color.White)
                            }

                            // Số lượng
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .width(90.dp)
                                    .height(60.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFE55C50)), // Màu đỏ cam
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = soLuong.toString(),
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            // Nút tăng
                            Button(
                                onClick = { if (soLuong < minOf(80, banConLai * 8)) soLuong++ },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                shape = CircleShape,
                                modifier = Modifier.size(56.dp).testTag("guests-plus")
                            ) {
                                Text("+", fontSize = 28.sp, color = Color.White)
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        // Ghi chú
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF404040)) // Nền tối hơn cho input
                                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                                .height(56.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_note), // Thay bằng drawable của bạn
                                contentDescription = "Ghi chú",
                                tint = Color.LightGray,
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(start = 12.dp)
                            )
                            BasicTextField(
                                value = ghiChu,
                                onValueChange = { ghiChu = it },
                                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, color = Color.White),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                decorationBox = { innerTextField ->
                                    if (ghiChu.isEmpty()) {
                                        Text(
                                            "Có trẻ nhỏ, thú cưng...",
                                            color = Color.Gray,
                                            fontSize = 16.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(40.dp))

                // Nút "Tiếp tục"
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp)) }
                Button(
                    enabled = !submitting && soLuong in 1..minOf(80, banConLai * 8),
                    onClick = {
                        onDatBan(soLuong, ghiChu)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE55C50)), // Màu đỏ cam
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("booking-submit")
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        if (submitting) "Đang giữ chỗ…" else "Đặt bàn",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    )
}
