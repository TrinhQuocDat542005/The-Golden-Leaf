package com.example.giaodien.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.giaodien.data.network.DemoSession
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.testTag

@Composable
fun DemoLoginScreen(onLoginSuccess: () -> Unit) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("The Golden Leaf · DEMO", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text("Dữ liệu giả lập · Không chuyển tiền · Không Firebase/FCM.\nBackend: ${com.example.giaodien.BuildConfig.API_BASE_URL}")
        Spacer(Modifier.height(16.dp))
        listOf("CUSTOMER" to "Khách demo · có đơn mẫu", "OTHER_CUSTOMER" to "Khách khác · kiểm tra tài khoản riêng").forEach { (persona, label) ->
            Button(enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = {
                busy = true; error = null
                scope.launch {
                    try {
                        DemoSession.signIn(persona)
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main.immediate) { onLoginSuccess() }
                    }
                    catch (e: CancellationException) { throw e }
                    catch (e: Exception) { DemoSession.recordFailure(e); error = "Chưa mở được phiên demo. Kiểm tra backend profile demo và thử lại." }
                    finally { busy = false }
                }
            }) { Text(label) }
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("demo-login-error")) }
        Spacer(Modifier.height(16.dp))
        Text("Đối soát/phân bàn: mở http://127.0.0.1:8080/staff.html trên máy tính. Restart backend để reset dữ liệu; đăng nhập lại sau khi reset.")
    }
}
