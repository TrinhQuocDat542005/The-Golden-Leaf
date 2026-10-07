package com.example.giaodien.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.giaodien.R
import com.example.giaodien.data.model.BanSlot
import com.example.giaodien.navigation.Screen
import com.example.giaodien.viewmodel.BanSlotViewModel
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SoDoBanScreen(
    navController: NavHostController,
    ngayChon: String,
    khungGioChon: String,
    banSlotViewModel: BanSlotViewModel = viewModel(),

) {
    // Lấy state từ ViewModel
    val slots by banSlotViewModel.slots.collectAsState()
    val daySlots: List<BanSlot> = slots.filter { slot ->
        slot.ngay.substring(0, 10) == ngayChon && slot.khungGio == khungGioChon
    }

    val selectedSlot = daySlots.firstOrNull()
    val total = selectedSlot?.soBanBanDau ?: 0
    val banConLai = selectedSlot?.soBanConLai ?: 0
    val banDaDat = total - banConLai
    val tables = (1..total).map { number -> TableVisual(number, number <= banDaDat) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // 1/3 trên: background + thông tin
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {

                Image(
                    painter = painterResource(id = R.drawable.bgcm),
                    contentDescription = "Background",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.3f)
                )

                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                        Text(
                            text = "Còn trống: $banConLai",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 28.sp,
                            color = Color(0xFF8B0000)
                        )



                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Ngày: $ngayChon",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.Black
                    )

                    Text(
                        text = "Khung giờ: $khungGioChon",
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Chú thích màu
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(Color.Gray, shape = RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Bàn đã đặt",
                            color = Color.Black,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.width(20.dp))

                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(Color.Green, shape = RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Bàn còn trống",
                            color = Color.Black,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // 2/3 dưới: sơ đồ bàn
            Box(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(horizontal = 8.dp, vertical = 16.dp)
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 70.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item { FacilityRow() }

                    var currentIndex = 0
                    while (currentIndex < tables.size) {
                        // Hàng 2 bàn
                        val row2Count = 2
                        if (currentIndex < tables.size) {
                            val tablesInRow2 = tables.subList(
                                currentIndex,
                                (currentIndex + row2Count).coerceAtMost(tables.size)
                            )
                            item { TableRow(slots = tablesInRow2) }
                            currentIndex += row2Count
                        }

                        // Hàng 3 bàn
                        val row3Count = 3
                        if (currentIndex < tables.size) {
                            val tablesInRow3 = tables.subList(
                                currentIndex,
                                (currentIndex + row3Count).coerceAtMost(tables.size)
                            )
                            item { TableRow(slots = tablesInRow3) }
                            currentIndex += row3Count
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                navController.navigate(
                    Screen.ViTriBan.createRoute(
                        ngayChon = ngayChon,
                        khungGioChon = khungGioChon,
                        banConLai = banConLai
                    )
                )
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF8B0000),
                contentColor = Color.White
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = "Tiếp tục",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun FacilityRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "🚪 Lối vào",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp
        )
        Text(
            text = "Toilet 🚻",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp
        )
    }
}

private data class TableVisual(val number: Int, val booked: Boolean)

@Composable
private fun TableRow(slots: List<TableVisual>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        slots.forEach { slot ->
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (slot.booked) Color.Gray else Color.Green
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "B${slot.number}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
