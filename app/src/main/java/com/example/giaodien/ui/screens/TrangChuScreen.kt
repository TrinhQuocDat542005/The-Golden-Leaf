@file:OptIn(ExperimentalFoundationApi::class)
package com.example.giaodien.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import com.example.giaodien.data.repository.NotificationRepository
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.giaodien.R
import com.example.giaodien.data.model.ThucDon
import com.example.giaodien.navigation.Screen
import com.example.giaodien.viewmodel.ThucDonViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.giaodien.ui.viewmodel.NotificationViewModel
import com.example.giaodien.viewmodel.NotificationViewModelFactory
import java.text.NumberFormat
import java.util.Locale
import com.example.giaodien.data.network.RetrofitInstance
import com.google.firebase.auth.FirebaseAuth

// --- Custom Colors ---
object AppColors {
    val CoralRed = Color(0xFFE9655D)
    val DarkBackground = Color(0xFF1E1E1E)
    val LightBackground = Color.White
    val LightText = Color(0xFFFFFFFF)
    val SemiDarkText = Color(0xFFCCCCCC)
    val SemiTransparentBlack = Color(0x66000000)
    val ImagePlaceholder = Color(0xFF8B0000)
    val EventPlaceholder = Color(0xFF006400)
    val FoodPlaceholder = Color.Gray
    val DiscountPriceColor = Color(0xFF6C6C6C)
}

private val categoryTabs = listOf("Nổi bật", "Món mới", "Giảm giá")
private const val MAX_ITEMS_TO_SHOW = 6

fun Double.toVND(): String {
    val format = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    return format.format(this).replace("₫", "₫ ")
}

// ----------------- Main Screen -----------------
@Composable
fun TrangChuScreen(
    navController: NavHostController,
    thucDonViewModel: ThucDonViewModel = viewModel(),
    notificationViewModel: NotificationViewModel

) {
    // Lấy api đã cấu hình sẵn


    val allThucDon by thucDonViewModel.thucDonList.collectAsState()
    val unreadCount by notificationViewModel.unreadCount.collectAsState()

    var selectedTab by remember { mutableStateOf(categoryTabs.first()) }

    val currentUser = FirebaseAuth.getInstance().currentUser
    // Load thực đơn 1 lần khi screen khởi tạo
    LaunchedEffect(Unit) {
        thucDonViewModel.loadThucDon()
    }


    var featuredList by remember { mutableStateOf<List<ThucDon>>(emptyList()) }
    var discountList by remember { mutableStateOf<List<ThucDon>>(emptyList()) }

    LaunchedEffect(allThucDon) {
        if (allThucDon.isNotEmpty()) {
            featuredList = allThucDon.shuffled().take(MAX_ITEMS_TO_SHOW)
            discountList = allThucDon.shuffled().take(MAX_ITEMS_TO_SHOW)
        }
    }

    val filteredThucDon = remember(selectedTab, featuredList, discountList, allThucDon) {
        when (selectedTab) {
            "Nổi bật" -> featuredList
            "Món mới" -> allThucDon.sortedByDescending { it.idThucDon }.take(MAX_ITEMS_TO_SHOW)
            "Giảm giá" -> discountList
            else -> emptyList()
        }
    }

    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
        Scaffold(
            topBar = { TopLocationBar(navController = navController, unreadCount = unreadCount) },
            bottomBar = { BottomNavBar(navController) },
            containerColor = AppColors.DarkBackground
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                LoveBanner()
                Spacer(modifier = Modifier.height(20.dp))
                CategoryTabs(
                    tabs = categoryTabs,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    navController = navController
                )
                Spacer(modifier = Modifier.height(20.dp))
                FoodListSection(
                    items = filteredThucDon,
                    isDiscountTab = selectedTab == "Giảm giá"
                )
                Spacer(modifier = Modifier.height(20.dp))
                UpcomingEventsSection()
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun TopLocationBar(navController: NavController, unreadCount: Int = 0) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                clip = false
            )
            .background(
                color = AppColors.DarkBackground,
                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = AppColors.CoralRed,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                val context = LocalContext.current
                val destination = "965 Quang Trung, quận Gò Vấp"
                Text(
                    text = destination,
                    modifier = Modifier
                        .clickable {
                            val encoded = Uri.encode(destination)

                            // Uri dẫn đường
                            val gmmIntentUri = Uri.parse("google.navigation:q=$encoded&mode=d")

                            // Intent mở Google Maps
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                            mapIntent.setPackage("com.google.android.apps.maps")

                            try {
                                context.startActivity(mapIntent)
                            } catch (e: ActivityNotFoundException) {
                                // Nếu không có Google Maps → mở bản đồ web
                                val browserIntent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$encoded")
                                )
                                context.startActivity(browserIntent)
                            }
                        }
                        .padding(8.dp),
                    color = Color.White ,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                // Icon chuông với badge
                Box(modifier = Modifier.size(48.dp)) {
                    IconButton(
                        onClick = { navController.navigate(Screen.Notification.route) },
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.Yellow
                        )
                    }

                    if (unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(19.dp)
                                .background(Color.Red, shape = CircleShape)
                                .align(Alignment.TopEnd),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = unreadCount.toString(),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }


            }
        }
    }
}



// ----------------- Love Banner -----------------
@Composable
fun LoveBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        Image(
            painter = painterResource(id = R.drawable.nhahang),
            contentDescription = "Valentine's Day Banner",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.SemiTransparentBlack)
        )
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(20.dp)
        ) {
            Text(
                text = "Celebrate Love at",
                color = AppColors.LightText,
                fontSize = 16.sp
            )
            Text(
                text = "The Golden Leaf",
                color = AppColors.LightText,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Treat your special\nsomeone our exclusive\nValentine's menu",
                color = AppColors.SemiDarkText,
                fontSize = 14.sp
            )
        }
    }
}

// ----------------- Category Tabs -----------------
@Composable
fun CategoryTabs(
    tabs: List<String>,
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    navController: NavController
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(tabs) { tab ->
                Text(
                    text = tab,
                    color = if (tab == selectedTab) AppColors.LightText else AppColors.SemiDarkText,
                    fontSize = 16.sp,
                    fontWeight = if (tab == selectedTab) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.clickable { onTabSelected(tab) }
                )
            }
        }

        // --- Nút Thực Đơn ---
        Button(
            onClick = { navController.navigate(Screen.ThucDon.route) },
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.CoralRed, contentColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Text("Thực Đơn", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}


// ----------------- Food List Section -----------------
@Composable
fun FoodListSection(items: List<ThucDon>, isDiscountTab: Boolean) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(items) { item -> FoodCard(item, isDiscountTab) }
    }
}

@Composable
fun FoodCard(item: ThucDon, isDiscountTab: Boolean) {
    var isFavorite by remember { mutableStateOf(false) }
    val actualPrice = item.gia
    val discountedPrice = if (isDiscountTab) actualPrice / 0.9 else 0.0

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.width(150.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                AsyncImage(
                    model = item.anh,
                    contentDescription = item.tenMon,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )


                IconButton(
                    onClick = { isFavorite = !isFavorite },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) AppColors.CoralRed else AppColors.LightText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = item.tenMon,
                    color = AppColors.LightText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (isDiscountTab) {
                    Text(
                        text = discountedPrice.toVND(),
                        color = AppColors.DiscountPriceColor,
                        fontSize = 10.sp,
                        textDecoration = TextDecoration.LineThrough,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = actualPrice.toVND(),
                        color = AppColors.SemiDarkText,
                        fontSize = 12.sp,
                        fontWeight = if (isDiscountTab) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "4.5",
                            color = AppColors.SemiDarkText,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

// ----------------- Upcoming Events -----------------
@Composable
fun UpcomingEventsSection() {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Upcoming Events",
                color = AppColors.LightText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = { }) {
                Text(
                    text = "See all",
                    color = AppColors.SemiDarkText,
                    fontSize = 14.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        EventItemCard(
            imageRes = R.drawable.game,
            title = "Game Night",
            description = "Không gian giải trí sôi động với nhiều hoạt động hấp dẫn...",
            seats = "2 Seats",
            time = "5-10-2025, 7:00 PM"
        )
        Spacer(modifier = Modifier.height(10.dp))
        EventItemCard(
            imageRes = R.drawable.game2,
            title = "Live Music Show",
            description = "Thưởng thức đêm nhạc sống lãng mạn với các ca sĩ nổi tiếng.",
            seats = "VIP Table",
            time = "10-10-2025, 8:00 PM"
        )
    }
}

@Composable
private fun EventItemCard(imageRes: Int, title: String, description: String, seats: String, time: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = "$title Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = AppColors.LightText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    description,
                    color = AppColors.SemiDarkText,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(seats, color = AppColors.CoralRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(time, color = AppColors.SemiDarkText, fontSize = 12.sp)
                }
            }
        }
    }
}

// ----------------- Bottom Nav -----------------
@Composable
fun BottomNavBar(navController: NavHostController) {
    Box(modifier = Modifier.fillMaxWidth()) {
        BottomAppBar(
            containerColor = AppColors.CoralRed,
            contentColor = AppColors.LightText,
            tonalElevation = 4.dp,
            modifier = Modifier.height(60.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(Icons.Default.Home, "Trang chủ", iconSize = 32.dp) {
                    navController.navigate(Screen.TrangChu.route)
                }



//                BottomNavItem(Icons.Default.Receipt, "Lịch sử đặt") { }
                Spacer(Modifier.width(60.dp))
//                BottomNavItem(Icons.Default.ChatBubble, "Tin Nhắn") {
//                    navController.navigate(Screen.Notification.route)
//                }

                BottomNavItem(Icons.Default.AccountCircle, "Tài khoản", iconSize = 32.dp) {
                    navController.navigate(Screen.TaiKhoan.route)
                }
            }
        }

        FloatingActionButton(
            onClick = { navController.navigate(Screen.NgayGio.route) },
            containerColor = AppColors.CoralRed,
            contentColor = AppColors.LightText,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-30).dp)
                .size(56.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(28.dp))
        }
    }
}
@Composable
private fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    iconSize: Dp = 24.dp, // default size
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = AppColors.LightText,
            modifier = Modifier.size(iconSize)
        )
    }
}
