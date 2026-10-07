package com.example.giaodien.notifications

import com.example.giaodien.data.model.DeviceTokenRequest
import com.example.giaodien.data.network.RetrofitInstance
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

/** Process-wide binding. Payload UID is also checked before rendering a push on shared devices. */
object DeviceBinding {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var boundUid: String? = null
    private var job: Job? = null
    fun start() {
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val uid = auth.currentUser?.uid
            if (uid != boundUid) {
                boundUid = uid; job?.cancel()
                job = scope.launch {
                    try {
                        if (uid == null) FirebaseMessaging.getInstance().deleteToken().await()
                        else {
                            val token = FirebaseMessaging.getInstance().token.await()
                            if (auth.currentUser?.uid == uid) RetrofitInstance.api.registerDevice(DeviceTokenRequest(token))
                        }
                    } catch (e: CancellationException) { throw e }
                    catch (_: Exception) { /* Offline: retry on next auth state change or app launch. */ }
                }
            }
        }
    }
}
