//package com.example.giaodien.ui.screens
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material3.*
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.getValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import com.example.giaodien.viewmodel.NotificationViewModel
//
//@Composable
//fun NotificationScreen(vm: NotificationViewModel) {
//    val notifications by vm.notifications.collectAsState()
//
//    if (notifications.isEmpty()) {
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(16.dp),
//            contentAlignment = Alignment.Center
//        ) {
//            Text(
//                text = "Chưa có thông báo nào",
//                fontSize = 18.sp,
//                color = Color.Gray
//            )
//        }
//    } else {
//        LazyColumn(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(vertical = 8.dp),
//            verticalArrangement = Arrangement.spacedBy(8.dp)
//        ) {
//            items(notifications) { noti ->
//                Card(
//                    shape = RoundedCornerShape(12.dp),
//                    colors = CardDefaults.cardColors(
//                        containerColor = if (!noti.readFlag) Color(0xFFE3F2FD) else Color.White
//                    ),
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(horizontal = 12.dp),
//                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
//                ) {
//                    Column(modifier = Modifier.padding(16.dp)) {
//                        Text(text = "Thông báo", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 16.sp)
//                        Spacer(modifier = Modifier.height(4.dp))
//                        Text(text = noti.message, color = Color.DarkGray)
//                        Spacer(modifier = Modifier.height(8.dp))
//                        if (!noti.readFlag) {
//                            Button(
//                                onClick = { vm.markRead(noti.id) },
//                                colors = ButtonDefaults.buttonColors(
//                                    containerColor = Color(0xFF1976D2),
//                                    contentColor = Color.White
//                                ),
//                                modifier = Modifier.align(Alignment.End)
//                            ) {
//                                Text("Đã đọc")
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
package com.example.giaodien.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.giaodien.ui.viewmodel.NotificationViewModel

@Composable
fun NotificationScreen(vm: NotificationViewModel) {
    val context = LocalContext.current
    val notifications by vm.notifications.collectAsState()
    val error by vm.errorMessage.collectAsState()

    // ⚡ Hiển thị toast cho lỗi, không đưa vào list
    error?.let {
        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
    }

    if (notifications.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Chưa có thông báo nào",
                fontSize = 18.sp,
                color = Color.Gray
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(notifications) { noti ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (!noti.readFlag) Color(0xFFE3F2FD) else Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Thông báo",
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = noti.message, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (!noti.readFlag) {
                            Button(
                                onClick = { vm.markRead(noti.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1976D2),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Đã đọc")
                            }
                        }
                    }
                }
            }
        }
    }
}
