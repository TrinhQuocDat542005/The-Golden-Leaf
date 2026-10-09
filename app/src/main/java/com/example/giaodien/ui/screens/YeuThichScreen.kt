@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.giaodien.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.giaodien.data.model.ThucDon
import com.example.giaodien.viewmodel.YeuThichViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

@Composable
fun YeuThichScreen(
    navController: NavController,
    yeuThichViewModel: YeuThichViewModel = viewModel()
) {
    val favoriteList by yeuThichViewModel.favoriteList.collectAsState(initial = emptyList())
    val loading by yeuThichViewModel.loading.collectAsState()
    val error by yeuThichViewModel.error.collectAsState()
    val userId = com.example.giaodien.data.network.CurrentAccount.user()?.uid ?: ""

    // Load danh sách yêu thích khi vào màn hình
    LaunchedEffect(userId) {
        yeuThichViewModel.loadFavorites(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Món Yêu Thích", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại", tint = Color.White)
                    }
                }
            )
        },
        containerColor = Color.Black
    ) { padding ->

        if (loading || error != null || favoriteList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                if (loading) CircularProgressIndicator() else if (error != null) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error!!, color = Color.White)
                    TextButton(onClick = { yeuThichViewModel.loadFavorites(userId) }) { Text("Thử lại") }
                } else Text(
                    "Danh sách yêu thích đang trống.",
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 8.dp)
            ) {
                items(favoriteList, key = { it.idThucDon }) { mon ->
                    FavoriteItemCard(item = mon, viewModel = yeuThichViewModel)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun FavoriteItemCard(item: ThucDon, viewModel: YeuThichViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            com.example.giaodien.ui.components.MenuImage(
                model = com.example.giaodien.data.model.menuImageUrl(item.anh, com.example.giaodien.BuildConfig.API_BASE_URL),
                contentDescription = item.tenMon,
                modifier = Modifier
                    .size(100.dp)
                    .clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop
            )



            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.tenMon, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(com.example.giaodien.data.model.formatVnd(item.gia), color = Color.White.copy(alpha = 0.7f))
            }
            IconButton(onClick = { viewModel.removeFavorite(item) }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove favorite",
                    tint = Color.Red
                )
            }
        }
    }
}
