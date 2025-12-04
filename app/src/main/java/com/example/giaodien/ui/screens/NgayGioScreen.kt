package com.example.giaodien.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.giaodien.R
import com.example.giaodien.data.model.BanSlot
import com.example.giaodien.navigation.Screen
import com.example.giaodien.viewmodel.BanSlotViewModel
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.DayOfWeek

val PrimaryRed = Color(0xFFE5584F)
val LightGreen = Color(0xFF8BC34A)

@Composable
fun NgayGioScreen(viewModel: BanSlotViewModel, navController: NavController) {
    // Đặt đoạn code này ngay đầu hàm NgayGioScreen
    LaunchedEffect(key1 = Unit) {
        // Tải lại dữ liệu mới nhất mỗi khi màn hình NgayGioScreen được hiển thị
        viewModel.fetchBanSlots()
    }
    val slots by viewModel.slots.collectAsState() // realtime
    val today = LocalDate.now()
    val weekDays = (0..6).map { today.plusDays(it.toLong()) }

    var selectedDate by remember { mutableStateOf(today) }
    var selectedKhungGio by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        HeaderBar(navController = navController)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            AddressSearch()
            Spacer(modifier = Modifier.height(16.dp))
            RestaurantInfo()
            Spacer(modifier = Modifier.height(16.dp))

            DateTimeSelectionBlock(
                weekDays = weekDays,
                selectedDate = selectedDate,
                slots = slots, // truyền trực tiếp slots
                selectedKhungGio = selectedKhungGio,
                onDateSelected = { newDate ->
                    selectedDate = newDate
                    selectedKhungGio = null
                },
                onSlotSelected = { khungGio ->
                    selectedKhungGio = khungGio
                }
            )
        }

        ContinueButton(
            isEnabled = selectedKhungGio != null,
            onClick = {
                // lọc realtime để lấy slot
                val daySlots = slots.filter { it.ngay.substring(0, 10) == selectedDate.toString() }
                val slot = daySlots.firstOrNull { it.khungGio == selectedKhungGio }
                slot?.let {
                    navController.navigate(
                        Screen.SoDoBan.createRoute(
                            ngayChon = selectedDate.toString(),
                            khungGioChon = it.khungGio
                        )
                    )
                }
            }
        )
    }
}
@Composable
fun DateTimeSelectionBlock(
    weekDays: List<LocalDate>,
    selectedDate: LocalDate,
    slots: List<BanSlot>,
    selectedKhungGio: String?,
    onDateSelected: (LocalDate) -> Unit,
    onSlotSelected: (String) -> Unit
) {
    val selectedDateStr = selectedDate.format(DateTimeFormatter.ISO_DATE)

    // 1. Lọc và nhóm các slot của ngày được chọn
    val slotsByKhungGio: Map<String, List<BanSlot>> = slots
        .filter { it.ngay.substring(0, 10) == selectedDateStr }
        .groupBy { it.khungGio }

    // 2. Tính tổng số bàn trống của ngày
    val totalAvailableTables = slotsByKhungGio.values.sumOf { list ->
        list.count { !it.daDat }
    }

    // 3. Lấy danh sách khung giờ duy nhất đã sắp xếp (ví dụ: theo thứ tự giờ)
    val khungGios = slotsByKhungGio.keys.sorted()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {

            // Chọn ngày trong tuần
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                items(weekDays) { date ->
                    val dayName = date.dayOfWeek.getVietnameseShortDay()
                    val isSelected = date == selectedDate
                    Text(
                        text = dayName,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) PrimaryRed else Color.Black,
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                            .clickable { onDateSelected(date) }
                    )
                }
            }

            // Header với số bàn trống (ĐÃ CẬP NHẬT)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val currentMonth = DateTimeFormatter.ofPattern("MM").format(selectedDate)
                val currentDayOfMonth = DateTimeFormatter.ofPattern("dd").format(selectedDate)
                Text(
                    text = "${selectedDate.dayOfWeek.getVietnameseDay()} $currentDayOfMonth tháng $currentMonth",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Trống: $totalAvailableTables", // Dùng giá trị đã tính toán tập trung
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF8B0000)
                )
            }

            // Danh sách khung giờ
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Sử dụng danh sách khung giờ đã được tính toán ở trên
                items(khungGios, key = { it }) { khungGio ->
                    val isSelected = khungGio == selectedKhungGio
                    val rowColor = if (isSelected) LightGreen.copy(alpha = 0.2f) else Color.Transparent

                    // Lấy số bàn trống cho khung giờ này
                    val availableSlotsInKhungGio = slotsByKhungGio[khungGio]?.count { !it.daDat } ?: 0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(rowColor)
                            .border(
                                2.dp,
                                if (isSelected) LightGreen else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onSlotSelected(khungGio) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = khungGio,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 16.sp,
                            color = if (isSelected) PrimaryRed else Color.Black
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(LightGreen)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = availableSlotsInKhungGio.toString(), // Dùng giá trị đã tính
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun HeaderBar(navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PrimaryRed)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_arrow_back),
            contentDescription = "Back",
            tint = Color.White,
            modifier = Modifier
                .size(28.dp)
                .clickable { navController.popBackStack() }
        )
        Text(
            text = "Khung giờ",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 140.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressSearch() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = "333 Tô Ký, Quận 12, tp.HCM.",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFEEEEEE),
                unfocusedContainerColor = Color(0xFFEEEEEE),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = Color.Black
            ),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Card(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = PrimaryRed)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = R.drawable.map),
                    contentDescription = "Map",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun RestaurantInfo() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.nhahang),
            contentDescription = "Restaurant Image",
            modifier = Modifier
                .size(220.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "THE SALT",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = "995 Quang Trung, Gò Vấp",
                fontSize = 24.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "feedback", fontSize = 22.sp, color = Color.Gray)
        }
    }
}

@Composable
fun ContinueButton(isEnabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = isEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryRed,
            disabledContainerColor = PrimaryRed.copy(alpha = 0.5f)
        )
    ) {
        Text(
            text = "Tiếp tục",
            color = Color.White,
            fontSize = 18.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

// Extensions
fun DayOfWeek.getVietnameseShortDay(): String = when (this) {
    DayOfWeek.MONDAY -> "Th2"
    DayOfWeek.TUESDAY -> "Th3"
    DayOfWeek.WEDNESDAY -> "Th4"
    DayOfWeek.THURSDAY -> "Th5"
    DayOfWeek.FRIDAY -> "Th6"
    DayOfWeek.SATURDAY -> "Th7"
    DayOfWeek.SUNDAY -> "CN"
}

fun DayOfWeek.getVietnameseDay(): String = when (this) {
    DayOfWeek.MONDAY -> "Thứ Hai"
    DayOfWeek.TUESDAY -> "Thứ Ba"
    DayOfWeek.WEDNESDAY -> "Thứ Tư"
    DayOfWeek.THURSDAY -> "Thứ Năm"
    DayOfWeek.FRIDAY -> "Thứ Sáu"
    DayOfWeek.SATURDAY -> "Thứ Bảy"
    DayOfWeek.SUNDAY -> "Chủ Nhật"
}
