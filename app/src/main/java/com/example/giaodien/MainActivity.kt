package com.example.giaodien

import android.os.Bundle
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
        FirebaseApp.initializeApp(this)
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                AppNavGraph(navController)
            }
        }
    }
}
