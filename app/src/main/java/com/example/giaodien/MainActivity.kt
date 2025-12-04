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

        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                AppNavGraph(navController)
            }
        }
    }
}
