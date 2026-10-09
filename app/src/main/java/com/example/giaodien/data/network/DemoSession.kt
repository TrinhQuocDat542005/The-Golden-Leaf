package com.example.giaodien.data.network

import android.net.Uri
import com.example.giaodien.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

data class AccountIdentity(val uid: String, val email: String?, val displayName: String?, val photoUrl: Uri? = null)

/** Separate demo APK only; synthetic credentials are never persisted or logged. */
object DemoSession {
    class State(val identity: AccountIdentity, val token: String) {
        override fun toString() = "DemoSession.State(redacted)"
    }
    @Volatile var state: State? = null
        private set
    private val observers = CopyOnWriteArrayList<() -> Unit>()
    // Diagnostic metadata only: never exception messages, credentials or response bodies.
    var failureTrace: String? = null
        private set
    fun recordFailure(error: Exception) {
        if (BuildConfig.DEBUG && BuildConfig.DEMO_MODE) failureTrace = error.javaClass.name + " at " +
            error.stackTrace.take(8).joinToString(" -> ") { "${it.className}.${it.methodName}:${it.lineNumber}" }
    }
    private val client = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
        .connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).build()
    fun observe(change: () -> Unit): () -> Unit {
        observers.add(change); change()
        return { observers.remove(change) }
    }
    fun signOut() { state = null; observers.forEach { it() } }
    suspend fun signIn(persona: String) = withContext(Dispatchers.IO) {
        failureTrace = null
        check(BuildConfig.DEBUG && BuildConfig.DEMO_MODE)
        require(persona in listOf("CUSTOMER", "OTHER_CUSTOMER", "STAFF", "ADMIN"))
        check(BuildConfig.API_BASE_URL in setOf("http://10.0.2.2:8080/", "http://10.0.2.2:18082/", "http://127.0.0.1:8080/", "http://127.0.0.1:18082/"))
        fun read(request: Request): JSONObject = client.newCall(request).execute().use {
            check(it.isSuccessful) { "Demo server unavailable" }
            JSONObject(it.body?.string() ?: error("Empty demo response"))
        }
        val config = read(Request.Builder().url(BuildConfig.API_BASE_URL + "api/demo/config").build())
        check(config.getBoolean("demo"))
        val result = read(Request.Builder().url(BuildConfig.API_BASE_URL + "api/demo/session")
            .post(JSONObject().put("persona", persona).toString().toRequestBody("application/json".toMediaType())).build())
        check(result.getBoolean("demo"))
        val profile = result.getJSONObject("profile")
        val name = when(persona) { "CUSTOMER" -> "Khách demo"; "OTHER_CUSTOMER" -> "Khách demo khác"; "STAFF" -> "Nhân viên demo"; else -> "Quản trị demo" }
        val next = State(AccountIdentity(profile.getString("uid"), profile.getString("email"), name), result.getString("token"))
        withContext(Dispatchers.Main) { state = next; observers.forEach { it() } }
    }
}

object CurrentAccount {
    fun user(): AccountIdentity? = if (BuildConfig.DEMO_MODE) DemoSession.state?.identity else
        FirebaseAuth.getInstance().currentUser?.let { AccountIdentity(it.uid, it.email, it.displayName, it.photoUrl) }
}
