package com.example.giaodien.ui.screens

import android.graphics.RenderEffect
import android.graphics.Shader
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.giaodien.R
import com.example.giaodien.viewmodel.LoginViewModel
import com.example.giaodien.viewmodel.LoginUiState

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
    onLoginSuccess: (String) -> Unit
) {
    Log.d("NavTest", "LoginScreen is displayed")

    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // 1️⃣ Background mờ + blur (không dùng blur())
        Image(
            painter = painterResource(id = R.drawable.bgcm),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize(),

            alpha = 0.7f
        )

        // Lớp phủ mờ đen
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.6f))
        )

        // 2️⃣ Cột chính
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 64.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 3️⃣ Logo
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo The Golden Leaf",
                modifier = Modifier
                    .size(400.dp)
                    .clip(RoundedCornerShape(20.dp))
            )

            Spacer(modifier = Modifier.height(64.dp))

            // 4️⃣ Card trắng mờ chứa Google Sign-In
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Đăng nhập để đặt bàn",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 5️⃣ Nút Google Sign-In
                    GoogleSignInButton(onSignInSuccess = { userEmail ->
                        onLoginSuccess(userEmail)
                    })

                    Spacer(modifier = Modifier.height(16.dp))

                    // 6️⃣ Hiển thị trạng thái login
                    when (uiState) {
                        is LoginUiState.Loading -> CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 4.dp
                        )
                        is LoginUiState.Error -> Text(
                            text = (uiState as LoginUiState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                        is LoginUiState.Success -> onLoginSuccess((uiState as LoginUiState.Success).userEmail)
                        else -> {}
                    }
                }
            }
        }
    }
}
