package com.example.giaodien.data.repository

import com.example.giaodien.data.model.Notification
import com.example.giaodien.data.network.ApiService

class NotificationRepository(private val api: ApiService) {

    suspend fun getUserNotifications(userId: Long?, userEmail: String?): List<Notification> =
        api.getNotifications(userId, userEmail)

    suspend fun markRead(id: Long) = api.markNotificationRead(id)
}
