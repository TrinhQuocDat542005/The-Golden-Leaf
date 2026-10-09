package com.example.giaodien.data.network

import com.google.firebase.auth.FirebaseAuth

/** Injectable identity boundary: tests never need a real Firebase account. */
interface AccountSession {
    fun uid(): String?
    fun observe(onChange: () -> Unit): () -> Unit
}
class FirebaseAccountSession : AccountSession {
    private val auth by lazy { FirebaseAuth.getInstance() }
    override fun uid() = CurrentAccount.user()?.uid
    override fun observe(onChange: () -> Unit): () -> Unit {
        if (com.example.giaodien.BuildConfig.DEMO_MODE) return DemoSession.observe(onChange)
        val listener = FirebaseAuth.AuthStateListener { onChange() }
        auth.addAuthStateListener(listener)
        return { auth.removeAuthStateListener(listener) }
    }
}
