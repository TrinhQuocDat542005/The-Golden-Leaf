package com.example.giaodien.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.giaodien.R
import com.example.giaodien.data.model.LichSuDonDayDuDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.giaodien.navigation.Screen
import com.example.giaodien.ui.viewmodel.TaiKhoanViewModel
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight // Đảm bảo import này được dùng đúng
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.core.LinearEasing

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.Dp


// Hàm giả định để format tiền tệ (giữ nguyên)
private fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    val formatted = format.format(amount)
    return formatted.replace("₫", "").trim() + " VND"
}

@Composable
fun TaiKhoanScreen(
    navController: NavController,
    viewModel: TaiKhoanViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            val repository = com.example.giaodien.data.repository.TaiKhoanRepository(com.example.giaodien.data.network.RetrofitInstance.api)
            return TaiKhoanViewModel(repository) as T
        }
    })
) {
    val user = FirebaseAuth.getInstance().currentUser
    val name = user?.displayName ?: "User"
    val email = user?.email ?: "Unknown email"
    val photoUrl = user?.photoUrl

    val choXacNhan by viewModel.choXacNhan.collectAsState()
    val lichSuDonDat by viewModel.lichSuDonDat.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF2F2F2))) {

        // ===== HEADER (ĐÃ CHỈNH SỬA) =====
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // Giảm chiều cao xuống để chỉ chứa nội dung ở phía trên
                .height(100.dp)
                .background(Color(0xFF800000))
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // Nút Quay lại - ĐÃ TĂNG KÍCH THƯỚC ICON
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(48.dp) // Tăng kích thước khu vực nhấn
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "Quay lại",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp) // Tăng kích thước icon
                )
            }

            // Hàng chứa Avatar, Tên và Email - CĂN CHỈNH PHẢI VÀ CÙNG HÀNG
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd), // Căn chỉnh toàn bộ khối này vào góc trên bên phải
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                // Column chứa Tên và Email - CĂN CHỈNH PHẢI
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = email,
                        fontSize = 14.sp,
                        color = Color(0xFFFFCDD2)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Avatar
                val avatarPainter = if (photoUrl != null) rememberAsyncImagePainter(photoUrl) else painterResource(id = R.drawable.bgcm)

                androidx.compose.foundation.Image(
                    painter = avatarPainter,
                    contentDescription = "User Avatar",
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentScale = ContentScale.Crop
                )
            }
        }
        // =======================================================

        // ===== NỘI DUNG (Sử dụng dữ liệu từ ViewModel) =====
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                // Cập nhật padding top để khớp với chiều cao mới của Header
                .padding(top = 120.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // CHO XÁC NHẬN: DÙNG LazyRow có nút điều khiển
                HorizontalScrollSection(
                    title = "Chờ Xác Nhận",
                    items = choXacNhan,
                    navController = navController,
                    viewModel = viewModel,
                    isLoading = isLoading
                )
            }
            item {
                // LỊCH SỬ ĐƠN ĐẶT: Giữ nguyên bố cục dọc
                SectionList(
                    title = "Lịch Sử Đơn Đặt",
                    items = lichSuDonDat,
                    navController = navController,
                    isCancellable = false,
                    viewModel = viewModel,
                    isLoading = isLoading
                )
            }
        }

        // ===== NÚT LOGOUT (Giữ nguyên) =====
        Button(
            onClick = {
                val context = navController.context
                FirebaseAuth.getInstance().signOut()
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
                val googleSignInClient = GoogleSignIn.getClient(context, gso)
                googleSignInClient.signOut().addOnCompleteListener {
                    navController.navigate("login") {
                        popUpTo("tai-khoan") { inclusive = true }
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(70.dp)
                .align(Alignment.BottomCenter)
                .padding(bottom = 15.dp)
        ) {
            Text(
                text = "Đăng xuất",
                fontSize = 18.sp,
                color = Color.White
            )
        }
    }
}


// ============== CÁC HÀM HỖ TRỢ (GIỮ NGUYÊN) ==============

// Composable mới để cuộn ngang và có nút điều khiển
@Composable
fun HorizontalScrollSection(
    title: String,
    items: List<LichSuDonDayDuDTO>,
    navController: NavController,
    viewModel: TaiKhoanViewModel,
    isLoading: Boolean

) {
    Column {
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF600000),
            modifier = Modifier.padding(start = 16.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (items.isEmpty()) {
            Text(
                text = "Không có đơn nào chờ xác nhận.",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp)
            )
            return
        }

        // 🔑 PHẦN LAZYROW VỚI NÚT ĐIỀU KHIỂN
        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()

        // --- SỬA LỖI Ở ĐÂY ---
        val isFirstItemVisible by remember {
            derivedStateOf {
                !listState.canScrollBackward
            }
        }
        val isLastItemVisible by remember {
            derivedStateOf {
                !listState.canScrollForward
            }
        }
        // -------------------------

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Nút điều khiển trái (Prev)
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        // Cuộn về item trước đó (Có thể cuộn 2 mục để cảm giác cuộn mượt hơn)
                        val prevIndex = (listState.firstVisibleItemIndex - 2).coerceAtLeast(0)
                        listState.animateScrollToItem(prevIndex)
                    }
                },
                enabled = !isFirstItemVisible, // Tắt nếu ở item đầu
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Filled.KeyboardArrowLeft,
                    contentDescription = "Trước",
                    tint = if (!isFirstItemVisible) Color(0xFF800000) else Color.LightGray
                )
            }

            // LazyRow chứa các thẻ (Card)
            LazyRow(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isLoading) {
                    items(4) {
                        SkeletonCard(
                            modifier = Modifier
                                .width(280.dp)
                                .height(160.dp)
                        )
                    }
                } else {
                    items(items) { item ->

                        // Thẻ đơn hàng (có thể hủy)
                        DonDatCard(
                            item = item,
                            navController = navController,
                            isCancellable = true, // Luôn là True cho mục này
                            viewModel = viewModel,
                            modifier = Modifier
                                .width(280.dp) // Kích thước cố định cho mỗi Card trong LazyRow
                                .height(IntrinsicSize.Min)
                        )
                    }
                }
            }


            // Nút điều khiển phải (Next)
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        // Cuộn tới item tiếp theo (Cuộn tới item đầu tiên + 2)
                        val nextIndex = (listState.firstVisibleItemIndex + 2).coerceAtMost(items.size - 1)
                        listState.animateScrollToItem(nextIndex)
                    }
                },
                enabled = !isLastItemVisible, // Tắt nếu ở item cuối
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Filled.KeyboardArrowRight,
                    contentDescription = "Sau",
                    tint = if (!isLastItemVisible) Color(0xFF800000) else Color.LightGray
                )
            }
        }
    }
}


// Composable giữ nguyên bố cục LazyColumn cũ (dùng cho Lịch Sử Đơn Đặt)
@Composable
fun SectionList(title: String, items: List<LichSuDonDayDuDTO>, navController: NavController, isCancellable: Boolean,
                viewModel: TaiKhoanViewModel, isLoading: Boolean) {
    Column {
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF600000),
            modifier = Modifier.padding(start = 16.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (items.isEmpty()) {
            Text(
                text = "Không có đơn nào.",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
            )
        }

        // LazyColumn cho các mục Lịch Sử (xếp dọc)
        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (isLoading) {
                repeat(4) {
                    SkeletonCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                }
            } else {
                items.forEach { item ->

                    DonDatCard(
                        item = item,
                        navController = navController,
                        isCancellable = isCancellable,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                    )
                }
            }
        }
    }
}


// Composable dùng chung cho việc hiển thị Card (đã tách ra để tái sử dụng)
@Composable
fun DonDatCard(
    item: LichSuDonDayDuDTO,
    navController: NavController,
    isCancellable: Boolean,
    viewModel: TaiKhoanViewModel,
    modifier: Modifier = Modifier
) {
    // 🔑 Trạng thái quản lý việc hiển thị hộp thoại xác nhận
    val showCancelDialog = remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Dòng 1: Ngày và ID Đơn
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Ngày: ${item.ngay}",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = "#: ${item.idDat}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))

            // Dòng 2: Khung giờ & Số bàn
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Khung giờ: ${item.khungGio}",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )

                if (item.soBan != null) {
                    Text(
                        text = "Bàn: ${item.soBan}",
                        color = Color(0xFFFFCDD2),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                } else {
                    Text(
                        text = "Chờ",
                        color = Color(0xFFFFCDD2),
                        fontSize = 14.sp
                    )
                }
            }

            // Thêm một đường kẻ chia cách
            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Hàng chứa Tổng tiền và các nút
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = " ${formatCurrency(item.tongTien ?: 0.0)}",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                )

                // Các nút Chi Tiết và Hủy Đặt (Căn phải)
                Row(horizontalArrangement = Arrangement.End) {
                    // Nút Chi Tiết
                    OutlinedButton(
                        onClick = {
                            navController.navigate(Screen.ChiTietHoaDon.createRoute(item.idDat))
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Xem", fontSize = 14.sp)
                    }

                    Spacer(Modifier.width(8.dp))

                    // Nút Hủy Đặt
                    if (isCancellable) {
                        Button(
                            onClick = {
                                // 🔑 Hiển thị hộp thoại xác nhận khi bấm Hủy
                                showCancelDialog.value = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE57373)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(
                                horizontal = 12.dp,
                                vertical = 6.dp
                            )
                        ) {
                            Text("Hủy", fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }

    // 🔑 Hộp thoại Xác nhận Hủy
    if (showCancelDialog.value) {
        AlertDialog(
            onDismissRequest = {
                // Đóng hộp thoại khi nhấn ra ngoài hoặc nhấn nút Thoát
                showCancelDialog.value = false
            },
            title = {
                Text(text = "Xác Nhận Hủy Đơn Đặt")
            },
            text = {
                Text(text = "Bạn có chắc chắn muốn hủy đơn đặt #${item.idDat} này không? Hành động này không thể hoàn tác.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // 1. Thực hiện hủy đơn đặt
                        viewModel.huyDonDat(item.idDat)
                        // 2. Đóng hộp thoại
                        showCancelDialog.value = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFB71C1C))
                ) {
                    Text("Chắc Chắn Hủy")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCancelDialog.value = false
                    }
                ) {
                    Text("Thoát")
                }
            }
        )
    }
}

@Composable
fun SkeletonCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E0E0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ShimmerBox(
                widthFraction = 0.6f,
                height = 20.dp,
                modifier = Modifier.clip(RoundedCornerShape(6.dp))
            )
            Spacer(Modifier.height(12.dp))
            ShimmerBox(
                widthFraction = 1f,
                height = 14.dp,
                modifier = Modifier.clip(RoundedCornerShape(6.dp))
            )
        }
    }
}

@Composable
fun ShimmerBox(modifier: Modifier = Modifier, widthFraction: Float = 1f, height: Dp) {
    val transition = rememberInfiniteTransition()
    val translate by transition.animateFloat(
        initialValue = -300f,
        targetValue = 300f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing))
    )

    val density = LocalDensity.current
    val heightPx = with(density) { height.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.LightGray.copy(0.25f),
                        Color.LightGray.copy(0.6f),
                        Color.LightGray.copy(0.25f)
                    ),
                    start = Offset(translate - 300f, 0f),
                    end = Offset(translate, heightPx)
                )
            )
    )
}
