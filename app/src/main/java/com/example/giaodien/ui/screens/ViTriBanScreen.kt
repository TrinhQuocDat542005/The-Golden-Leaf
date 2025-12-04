package com.example.giaodien.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.giaodien.R
import com.example.giaodien.viewmodel.WeatherViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

@Composable
fun ViTriBanScreen(
    ngayChon: String,          // yyyy-MM-dd
    khungGioChon: String,      // ví dụ "07:00-11:00"
    weatherViewModel: WeatherViewModel,
    onBack: () -> Unit = {},
    onNext: (viTriBan: String) -> Unit = {}
) {
    // --- Parse khung giờ ---
    val khungParts = khungGioChon.split("-")
    val khungStartHour = khungParts.getOrNull(0)?.substringBefore(":")?.toIntOrNull() ?: 0
    val khungEndHour = khungParts.getOrNull(1)?.substringBefore(":")?.toIntOrNull() ?: 24

    // --- Load forecast từ ViewModel ---
    val forecastState by weatherViewModel.forecast.collectAsState()
    LaunchedEffect(Unit) { weatherViewModel.loadForecast("Hanoi") }

    // --- Lọc theo ngày + khung giờ ---
    val filteredWeather = remember(forecastState, ngayChon, khungStartHour, khungEndHour) {
        forecastState?.list?.filter { entry ->
            val dateTime = Instant.ofEpochSecond(entry.dt)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
            val entryDate = dateTime.toLocalDate()
            val entryHour = dateTime.hour

            val isTimeMatch = entryHour >= khungStartHour && entryHour < khungEndHour

            entryDate.toString() == ngayChon && isTimeMatch
        } ?: emptyList()
    }

    // --- Vị trí bàn ---
    val danhSachViTri = listOf("Ngoài trời", "Sông hồ", "Trong nhà", "Phòng riêng")
    val danhSachAnh = listOf(
        R.drawable.ngoaitroi,
        R.drawable.songho,
        R.drawable.trongnha,
        R.drawable.phongrieng
    )
    var viTriDaChon by remember { mutableStateOf(danhSachViTri[2]) }

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(viTriDaChon) {
        val index = danhSachViTri.indexOf(viTriDaChon)
        if (index != -1) {
            coroutineScope.launch { lazyListState.animateScrollToItem(index) }
        }
    }

    // Đặt padding dưới cùng cho LazyColumn để nút "Tiếp tục" không bị che
    val bottomPaddingForButton = 80.dp

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Color(0xFFE8544D))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(48.dp) // Đảm bảo khu vực nhấn đủ lớn
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp) // Tăng kích thước Icon lên 32.dp
                    )
                }
                Text(
                    "Vị trí bàn",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(24.dp))
            }
        },
        containerColor = Color(0xFF282828)
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            // --- 1. LazyColumn (Nội dung có thể cuộn) ---
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 0.dp,
                    bottom = bottomPaddingForButton, // Tạo khoảng trống cho nút cố định
                    start = 0.dp,
                    end = 0.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // 1.1. --- Thời tiết (ĐÃ ĐƠN GIẢN HÓA CẤU TRÚC) ---
                item {
                    val forecastData = filteredWeather
                    val ngayHienThi = ngayChon.split("-").reversed().joinToString("/")

                    val averageTemp = if (forecastData.isNotEmpty()) {
                        forecastData.map { it.main.temp }.average().toInt()
                    } else {
                        null
                    }

                    val description = forecastData.firstOrNull()?.weather?.firstOrNull()?.description
                        ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                        ?: "Không rõ"

                    Column( // Thay vì Box lớn, dùng Column trực tiếp cho nội dung
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .background(Color(0xFF3A3A3A), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                            .shadow(4.dp, RoundedCornerShape(12.dp))
                    ) {
                        if (averageTemp == null) {
                            Text("Đang tải hoặc không có dữ liệu thời tiết", color = Color.White)
                        } else {
                            // Hàng 1: Ngày và Khung giờ
                            Text("📅 Ngày: $ngayHienThi", color = Color.LightGray, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "⏰ Khung giờ: ${"%02d:00 - %02d:00".format(khungStartHour, khungEndHour)}",
                                color = Color.LightGray,
                                fontSize = 14.sp
                            )

                            Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFF555555))

                            // Hàng 2: Nhiệt độ và Mô tả
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Cột 1: Nhiệt độ Trung bình
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Thermostat,
                                        contentDescription = "Nhiệt độ",
                                        tint = Color(0xFFFDD835),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "$averageTemp°C",
                                        color = Color.White,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }

                                // Cột 2: Mô tả Thời tiết
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "Thời tiết:",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 12.sp,
                                    )
                                    Text(
                                        description,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                } // END item 1.1

                // 1.2. --- LazyRow ảnh vị trí (GIỮ NGUYÊN) ---
                item {
                    LazyRow(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(danhSachAnh) { index, drawableId ->
                            val isSelected = viTriDaChon == danhSachViTri[index]
                            Box(
                                modifier = Modifier
                                    .width(300.dp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(20.dp))
                                    .shadow(8.dp, RoundedCornerShape(20.dp))
                                    .background(Color.DarkGray)
                                    .clickable { viTriDaChon = danhSachViTri[index] }
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0xFFE8544D).copy(alpha = 0.4f))
                                    )
                                }
                                Image(
                                    painter = painterResource(id = drawableId),
                                    contentDescription = danhSachViTri[index],
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                                    contentAlignment = Alignment.BottomStart
                                ) {
                                    Text(
                                        danhSachViTri[index],
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                } // END item 1.2

                // 1.3. --- RadioButton chọn vị trí (GIỮ NGUYÊN) ---
                itemsIndexed(danhSachViTri) { _, viTri ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clickable { viTriDaChon = viTri }
                            .padding(vertical = 4.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = viTri,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium
                        )
                        RadioButton(
                            selected = viTriDaChon == viTri,
                            onClick = { viTriDaChon = viTri },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color.White,
                                unselectedColor = Color.White.copy(alpha = 0.7f)
                            )
                        )
                    }
                } // END items 1.3
            } // END LazyColumn

            // --- 2. Nút Tiếp tục CỐ ĐỊNH dưới cùng (Fixed Button) ---
            Button(
                onClick = { onNext(viTriDaChon) },
                enabled = viTriDaChon.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8544D)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter) // Cố định ở dưới cùng Box chứa
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Text(
                    "Tiếp tục",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            } // END Fixed Button
        }
    }
}