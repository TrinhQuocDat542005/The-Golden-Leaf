package com.example.giaodien.data.network

import com.google.firebase.auth.FirebaseAuth

/** Injectable identity boundary: tests never need a real Firebase account. */
interface AccountSession {
    fun uid(): String?
    fun observe(onChange: () -> Unit): () -> Unit
}
class FirebaseAccountSession : AccountSession {
    private val auth = FirebaseAuth.getInstance()
    override fun uid() = auth.currentUser?.uid
    override fun observe(onChange: () -> Unit): () -> Unit {
        val listener = FirebaseAuth.AuthStateListener { onChange() }
        auth.addAuthStateListener(listener)
        return { auth.removeAuthStateListener(listener) }
    }
}
