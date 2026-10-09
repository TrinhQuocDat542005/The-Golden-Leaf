package com.example.giaodien.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.giaodien.ui.viewmodel.NotificationViewModel

@Composable
fun NotificationScreen(vm: NotificationViewModel) {
    val notifications by vm.notifications.collectAsState()
    val error by vm.errorMessage.collectAsState()
    val loading by vm.loading.collectAsState()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Thông báo", style = MaterialTheme.typography.headlineSmall) }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        error?.let { message -> item {
            Text(message, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = vm::retry, enabled = !loading) { Text("Thử lại") }
        } }
        if (!loading && error == null && notifications.isEmpty()) item { Text("Chưa có thông báo nào") }
        items(notifications, key = { it.id }) { notification ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(notification.message)
                    if (!notification.readFlag) TextButton(onClick = { vm.markRead(notification.id) }) { Text("Đánh dấu đã đọc") }
                }
            }
        }
    }
}
