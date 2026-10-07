package com.example.giaodien.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.example.giaodien.MainActivity
import com.example.giaodien.R
import com.example.giaodien.data.model.DeviceTokenRequest
import com.example.giaodien.data.network.RetrofitInstance
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.*

class RestaurantMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    override fun onNewToken(token: String) {
        if (FirebaseAuth.getInstance().currentUser != null) scope.launch {
            try { RetrofitInstance.api.registerDevice(DeviceTokenRequest(token)) } catch (_: Exception) { /* Retry at next sign-in. */ }
        }
    }
    override fun onMessageReceived(message: RemoteMessage) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (message.data["userUid"] != uid) return
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val channel = "restaurant_updates"
        if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(channel, "Cập nhật đặt bàn", NotificationManager.IMPORTANCE_DEFAULT))
        val id = message.data["notificationId"]?.toLongOrNull()?.toInt() ?: return
        val intent = PendingIntent.getActivity(this, id, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, channel).setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(message.data["title"] ?: "The Golden Leaf")
            .setContentText(message.data["message"] ?: "Đơn đặt bàn được cập nhật")
            .setContentIntent(intent).setAutoCancel(true).setOnlyAlertOnce(true).build()
        NotificationManagerCompat.from(this).notify(id, notification)
    }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
