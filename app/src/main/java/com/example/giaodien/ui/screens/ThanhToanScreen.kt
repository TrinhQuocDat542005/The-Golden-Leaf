package com.example.giaodien.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.giaodien.viewmodel.HoaDonViewModel
import kotlinx.coroutines.delay
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun ThanhToanScreen(navController: NavController, method: String, viewModel: HoaDonViewModel) {
    val payment by viewModel.payment.collectAsState()
    val processing by viewModel.processing.collectAsState()
    val error by viewModel.error.collectAsState()
    LaunchedEffect(viewModel.idDat) { viewModel.thanhToan("Ngân hàng") }
    LaunchedEffect(payment?.status) {
        while (payment?.status == "PENDING") { delay(15000); viewModel.refreshPayment() }
    }
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Thanh toán chuyển khoản", style = MaterialTheme.typography.headlineSmall)
            Text("Đơn #${viewModel.idDat}")
            payment?.let { p ->
                Card { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("vi-VN")).apply { maximumFractionDigits = 2 }.format(p.amount)} ${p.currency}", style = MaterialTheme.typography.headlineMedium)
                    Text("Ngân hàng: ${p.bankName}")
                    Text("Số tài khoản: ${p.accountNumber}")
                    Text("Chủ tài khoản: ${p.accountName}")
                    Text("Nội dung chuyển khoản: ${p.reference}", style = MaterialTheme.typography.titleMedium)
                    if (p.accountNumber.isBlank()) Text("Chưa có thông tin tài khoản nhận tiền. Liên hệ nhà hàng trước khi chuyển.", color = MaterialTheme.colorScheme.error)
                } }
                Text(when (p.status) {
                    "PENDING" -> if (com.example.giaodien.BuildConfig.DEMO_MODE) "Giao dịch giả lập. KHÔNG chuyển tiền. Mở staff.html trên máy tính để xác nhận mẫu." else "Chờ nhân viên đối soát. Chuyển đúng số tiền và nội dung trên; không chuyển lần nữa nếu đã gửi tiền."
                    "PAID" -> "Nhà hàng đã xác nhận nhận tiền."
                    "REFUND_REQUIRED" -> "Đơn đã hủy, đang chờ nhà hàng hoàn tiền."
                    "REFUNDED" -> "Nhà hàng đã ghi nhận hoàn tiền."
                    "CANCELLED" -> "Yêu cầu thanh toán đã hủy. Không tiếp tục chuyển tiền."
                    else -> p.status
                })
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(enabled = !processing, onClick = { if (payment == null) viewModel.thanhToan("Ngân hàng") else viewModel.refreshPayment() }) {
                Text(if (processing) "Đang kiểm tra…" else "Kiểm tra trạng thái")
            }
            TextButton(onClick = { navController.navigate("trang_chu") { launchSingleTop = true } }) { Text("Về trang chủ") }
            Text("Không có QR mẫu hoặc thanh toán MoMo/VNPay giả. Chỉ xác nhận của nhà hàng mới đổi trạng thái sang đã thanh toán.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
