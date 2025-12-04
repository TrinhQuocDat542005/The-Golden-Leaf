package com.example.giaodien.ui.screens

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.Color
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.example.giaodien.R

// (Loại bỏ MaroonColor)

@Composable
fun GoogleSignInButton(
    onSignInSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val firebaseAuth = FirebaseAuth.getInstance()

    // Khởi tạo Main Thread Handler
    val mainHandler = Handler(Looper.getMainLooper())

    // LẤY TỪ CẤU HÌNH CỦA BẠN: Client Type 3 (Web Client ID)
    val webClientId = "751205260991-460l4lns5mfi8fk3bpm5mg4igh9suhos.apps.googleusercontent.com"

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(webClientId)
        .requestEmail()
        .build()

    val googleSignInClient = remember {
        GoogleSignIn.getClient(context, gso)
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)

        try {
            // 1. Lấy Google Sign-In Account
            val account = task.getResult(ApiException::class.java)
            val googleIdToken = account.idToken

            if (googleIdToken == null) {
                Log.e("GoogleSignIn", "Lỗi: idToken null sau khi đăng nhập Google thành công.")
                Toast.makeText(context, "Lỗi xác thực: Không nhận được ID Token.", Toast.LENGTH_LONG).show()
                return@rememberLauncherForActivityResult
            }

            val credential = GoogleAuthProvider.getCredential(googleIdToken, null)

            // 2. Đăng nhập Firebase bằng Credential
            firebaseAuth.signInWithCredential(credential).addOnCompleteListener { authResult ->
                if (authResult.isSuccessful) {
                    val firebaseUser = firebaseAuth.currentUser
                    val userEmail = firebaseUser?.email ?: "email-not-found"
                    Log.d("FirebaseAuth", "Đăng nhập Firebase thành công cho user: $userEmail. Bắt đầu lấy Firebase ID Token.")

                    // 3. Lấy Firebase ID Token (có "aud" là Project ID)
                    firebaseUser?.getIdToken(true)?.addOnCompleteListener { tokenTask ->
                        if (tokenTask.isSuccessful) {
                            val firebaseIdToken = tokenTask.result?.token
                            val jsonBody = JSONObject().apply {
                                put("idToken", firebaseIdToken)
                            }

                            val body = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                            if (firebaseIdToken != null) {
                                Log.d("FirebaseAuth", "Thành công lấy Firebase ID Token. Bắt đầu Backend Sync.")

                                // 4. Đồng bộ hóa với Backend (OkHttpClient)
                                val request = Request.Builder()
                                   // .url("http://10.0.2.2:8080/api/auth/sync")
                                    .url("http://10.0.2.2:8080/api/auth/sync")
                                    .post(body) // POST mới đúng
                                    .build()


                                // Gửi Firebase ID Token lên Backend
                                OkHttpClient().newCall(request).enqueue(object : Callback {
                                    override fun onResponse(call: Call, response: Response) {
                                        if (response.isSuccessful) {
                                            Log.d("BackendSync", "Đồng bộ hóa backend thành công.")

                                            // 💡 CHẠY NAVIGATION TRÊN MAIN THREAD
                                            mainHandler.post {
                                                onSignInSuccess(userEmail)
                                            }

                                        } else {
                                            Log.e("BackendSync", "Đồng bộ hóa backend thất bại: Code ${response.code}, Message: ${response.body?.string()}")
                                            // Thường không nên gọi Toast từ đây, nhưng nếu cần:
                                            // mainHandler.post { Toast.makeText(context, "Lỗi backend ${response.code}", Toast.LENGTH_LONG).show() }
                                        }
                                    }

                                    override fun onFailure(call: Call, e: IOException) {
                                        Log.e("BackendSync", "Lỗi mạng khi đồng bộ hóa backend: ${e.message}")
                                    }
                                })
                            } else {
                                Log.e("FirebaseAuth", "Lỗi: Firebase ID Token null.")
                                Toast.makeText(context, "Lỗi: Không thể tạo Firebase ID Token.", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Log.e("FirebaseAuth", "Lỗi khi lấy Firebase ID Token: ${tokenTask.exception?.localizedMessage}")
                            Toast.makeText(context, "Lỗi: Không thể lấy Firebase ID Token.", Toast.LENGTH_LONG).show()
                        }
                    }

                } else {
                    // Firebase Sign-In thất bại
                    Log.e("FirebaseAuth", "Đăng nhập Firebase thất bại: ${authResult.exception?.message}")
                    Toast.makeText(context, "Đăng nhập Firebase thất bại. Kiểm tra Logcat.", Toast.LENGTH_LONG).show()
                }
            }

        } catch (e: ApiException) {
            // Google Sign-In API Exception
            Log.e("GoogleSignIn", "API Exception during Google Sign-In: Status Code ${e.statusCode}, Message: ${e.message}")
            if (e.statusCode == 12500) {
                Toast.makeText(context, "Lỗi 12500: Lỗi cấu hình SHA-1. Hãy kiểm tra lại Firebase Console.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Đăng nhập Google thất bại (Code: ${e.statusCode})", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            // General Exception
            Log.e("GoogleSignIn", "Lỗi chung khi xử lý kết quả: ${e.message}")
            Toast.makeText(context, "Đăng nhập Google thất bại: Lỗi chung.", Toast.LENGTH_LONG).show()
        }
    }

    Button(
        onClick = { launcher.launch(googleSignInClient.signInIntent) },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Red.copy(alpha = 0.6f),
            contentColor = Color.White
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Icon Google
            Image(
                painter = painterResource(id = R.drawable.gg_logo),
                contentDescription = "Google Logo",
                modifier = Modifier
                    .size(24.dp) // kích thước icon
            )

            Spacer(modifier = Modifier.width(8.dp)) // khoảng cách giữa icon và text

            Text("Đăng nhập bằng Google")
        }
    }

}