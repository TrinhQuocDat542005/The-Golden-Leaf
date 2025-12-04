    @file:OptIn(ExperimentalMaterial3Api::class)

    package com.example.giaodien.ui.screens

    import androidx.compose.foundation.Image
    import androidx.compose.foundation.clickable
    import androidx.compose.foundation.layout.*
    import androidx.compose.foundation.lazy.LazyColumn
    import androidx.compose.foundation.lazy.LazyRow
    import androidx.compose.foundation.lazy.items
    import androidx.compose.foundation.shape.RoundedCornerShape
    import androidx.compose.material3.*
    import androidx.compose.material3.Icon
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.filled.ArrowBack
    import androidx.compose.material.icons.filled.Favorite
    import androidx.compose.material.icons.filled.Search
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
    import androidx.lifecycle.viewmodel.compose.viewModel
    import androidx.navigation.NavController
    import coil.compose.rememberAsyncImagePainter
    import com.example.giaodien.R
    import com.example.giaodien.data.model.ThucDon
    import com.example.giaodien.navigation.Screen
    import com.example.giaodien.viewmodel.ThucDonViewModel
    import com.example.giaodien.viewmodel.YeuThichViewModel
    import com.google.firebase.auth.FirebaseAuth
    import java.util.*

    fun String.removeVietnameseAccents(): String {
        var str = this.lowercase(Locale.ROOT)
        str = str.replace('à', 'a').replace('á', 'a').replace('ả', 'a').replace('ã', 'a').replace('ạ', 'a')
        str = str.replace('ă', 'a').replace('ằ', 'a').replace('ắ', 'a').replace('ẳ', 'a').replace('ẵ', 'a').replace('ặ', 'a')
        str = str.replace('â', 'a').replace('ầ', 'a').replace('ấ', 'a').replace('ẩ', 'a').replace('ẫ', 'a').replace('ậ', 'a')
        str = str.replace('è', 'e').replace('é', 'e').replace('ẻ', 'e').replace('ẽ', 'e').replace('ẹ', 'e')
        str = str.replace('ê', 'e').replace('ề', 'e').replace('ế', 'e').replace('ể', 'e').replace('ễ', 'e').replace('ệ', 'e')
        str = str.replace('ì', 'i').replace('í', 'i').replace('ỉ', 'i').replace('ĩ', 'i').replace('ị', 'i')
        str = str.replace('ò', 'o').replace('ó', 'o').replace('ỏ', 'o').replace('õ', 'o').replace('ọ', 'o')
        str = str.replace('ô', 'o').replace('ồ', 'o').replace('ố', 'o').replace('ổ', 'o').replace('ỗ', 'o').replace('ộ', 'o')
        str = str.replace('ơ', 'o').replace('ờ', 'o').replace('ớ', 'o').replace('ở', 'o').replace('ỡ', 'o').replace('ợ', 'o')
        str = str.replace('ù', 'u').replace('ú', 'u').replace('ủ', 'u').replace('ũ', 'u').replace('ụ', 'u')
        str = str.replace('ư', 'u').replace('ừ', 'u').replace('ứ', 'u').replace('ử', 'u').replace('ữ', 'u').replace('ự', 'u')
        str = str.replace('ỳ', 'y').replace('ý', 'y').replace('ỷ', 'y').replace('ỹ', 'y').replace('ỵ', 'y')
        str = str.replace('đ', 'd')
        return str.replace(' ', ' ').trim()
    }
    @Composable
    fun ThucDonScreen(
        navController: NavController,
        thucDonViewModel: ThucDonViewModel = viewModel(),
        yeuThichViewModel: YeuThichViewModel = viewModel()
    ) {
        val thucDonList by thucDonViewModel.thucDonList.collectAsState(initial = emptyList())
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val DeepRed = Color(0xFF8B0000)

        // 1. Thêm state cho thanh tìm kiếm
        var searchText by remember { mutableStateOf("") }

        // 2. Hàm lọc danh sách món ăn dựa trên searchText
        val filteredThucDonList = remember(thucDonList, searchText) {
            if (searchText.isBlank()) {
                thucDonList
            } else {
                // Chuẩn hóa từ khóa tìm kiếm (bỏ dấu và chữ thường)
                val normalizedSearchText = searchText.removeVietnameseAccents()

                thucDonList.filter { thucDon ->
                    // Chuẩn hóa tên món và nhóm món
                    val normalizedTenMon = thucDon.tenMon.removeVietnameseAccents()
                    val normalizedNhom = thucDon.nhom.removeVietnameseAccents()

                    // Tìm kiếm trên cả Tên món HOẶC Nhóm món đã chuẩn hóa
                    normalizedTenMon.contains(normalizedSearchText) || normalizedNhom.contains(normalizedSearchText)
                }
            }
        }


        LaunchedEffect(true) {
            thucDonViewModel.loadThucDon()
            yeuThichViewModel.loadFavorites(userId)
        }


        Scaffold(containerColor = Color.Black) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {

                    // Header - KHÔNG ĐỔI
                    val zeroCorner = androidx.compose.foundation.shape.CornerSize(0.dp)
                    val mediumShape = MaterialTheme.shapes.medium as RoundedCornerShape
                    val headerShape = RoundedCornerShape(
                        topStart = zeroCorner,
                        topEnd = zeroCorner,
                        bottomStart = mediumShape.bottomStart,
                        bottomEnd = mediumShape.bottomEnd
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(headerShape)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.bgcm),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = 0.6f
                        )

                        IconButton(
                            onClick = { navController.navigate(Screen.TrangChu.route) },
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại", tint = Color.White)
                        }
                        // Text ở giữa Box
                        Text(
                            text = "The Kitchen\nBy The River",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp
                            ),
                            color = Color.White,
                            modifier = Modifier.align(Alignment.Center)
                        )

                        // IconButton ở góc trên bên phải
                        IconButton(
                            onClick = { navController.navigate(Screen.YeuThich.route) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Yêu Thích",
                                tint = Color.Yellow
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- THANH TÌM KIẾM MỚI ---
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        label = { Text("Tìm kiếm món (Tên hoặc Nhóm)", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Tìm kiếm") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E1E1E),
                            unfocusedContainerColor = Color(0xFF1E1E1E),
                            focusedBorderColor = DeepRed,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedLeadingIconColor = DeepRed,
                            unfocusedLeadingIconColor = Color.White,
                            focusedLabelColor = DeepRed,
                            unfocusedLabelColor = Color.White,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    // -----------------------------

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .weight(1f)
                            .padding(vertical = 8.dp)
                    ) {
                        // Cập nhật để sử dụng filteredThucDonList
                        item {
                            MonAnRowYeuThich(
                                "Món khai vị",
                                // Lọc trên danh sách đã được lọc bởi thanh tìm kiếm
                                filteredThucDonList.filter { it.nhom.lowercase() == "khai_vi" },
                                yeuThichViewModel,
                                userId,
                                navController
                            )
                        }
                        item {
                            MonAnRowYeuThich(
                                "Món chính",
                                // Lọc trên danh sách đã được lọc bởi thanh tìm kiếm
                                filteredThucDonList.filter { it.nhom.lowercase() == "mon_chinh" },
                                yeuThichViewModel,
                                userId,
                                navController
                            )
                        }
                        item {
                            MonAnRowYeuThich(
                                "Món tráng miệng",
                                // Lọc trên danh sách đã được lọc bởi thanh tìm kiếm
                                filteredThucDonList.filter { it.nhom.lowercase() == "trang_mieng" },
                                yeuThichViewModel,
                                userId,
                                navController
                            )
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
    // Hàm tiện ích để loại bỏ dấu tiếng Việt và chuyển sang chữ thường


    @Composable
    fun MonAnRowYeuThich(
        title: String,
        monList: List<ThucDon>,
        yeuThichViewModel: YeuThichViewModel,
        userId: String,
        navController: NavController
    ) {
        val DeepRed = Color(0xFF8B0000)

        // Chỉ hiển thị nhóm món này nếu có món ăn nào đó trong nhóm đó sau khi lọc
        if (monList.isNotEmpty()) {
            val favoriteList by yeuThichViewModel.favoriteList.collectAsState(initial = emptyList<ThucDon>())

            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.padding(start = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(monList, key = { it.idThucDon }) { mon ->

                        val isFavorite = favoriteList.any { it.idThucDon == mon.idThucDon }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .width(140.dp)
                                .padding(vertical = 4.dp)
                                .clickable {
                                    // Chuyển sang MoTaScreen, truyền id món
                                    navController.navigate("mo_ta/${mon.idThucDon}")
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Image(
                                    painter = rememberAsyncImagePainter(mon.anh),
                                    contentDescription = mon.tenMon,
                                    modifier = Modifier
                                        .height(80.dp)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(mon.tenMon, style = MaterialTheme.typography.bodyMedium, color = Color.White, maxLines = 1)
                                Text("${mon.gia} VND", style = MaterialTheme.typography.bodySmall, color = Color.Green.copy(alpha = 0.8f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = { yeuThichViewModel.toggleFavorite(userId, mon) },
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepRed),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Filled.Favorite,
                                        contentDescription = null,
                                        tint = if (isFavorite) Color.Yellow else Color.White
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (isFavorite) "Đã YT" else "Yêu Thích")
                                }
                            }
                        }
                    }
                }
            }
        }
    }