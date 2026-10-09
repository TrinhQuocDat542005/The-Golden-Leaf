package com.example.giaodien

import android.os.Bundle
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.navigation.compose.rememberNavController
import com.example.giaodien.navigation.AppNavGraph
import com.google.firebase.FirebaseApp
import dagger.hilt.android.AndroidEntryPoint  // ✅ thêm import

@AndroidEntryPoint // ✅ bắt buộc để Hilt inject được ViewModel
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BuildConfig.DEMO_MODE) FirebaseApp.initializeApp(this)
        if (!BuildConfig.DEMO_MODE && android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                androidx.compose.foundation.layout.Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
                    if (BuildConfig.DEMO_MODE) androidx.compose.material3.Text(
                        "DEMO · Dữ liệu giả lập · Không chuyển tiền",
                        color = androidx.compose.ui.graphics.Color(0xFFB71C1C),
                        style = MaterialTheme.typography.labelLarge
                    )
                    androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.weight(1f)) {
                        AppNavGraph(navController)
                    }
                }
            }
        }
    }
}
