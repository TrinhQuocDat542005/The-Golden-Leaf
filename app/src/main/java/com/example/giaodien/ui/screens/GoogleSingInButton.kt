package com.example.giaodien.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.giaodien.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

/** Authenticate once, then use LoginViewModel's shared backend-sync/error flow. */
@Composable
fun GoogleSignInButton(onSignInSuccess: (String) -> Unit) {
    val context=LocalContext.current
    var busy by remember { mutableStateOf(false) }
    val callback by rememberUpdatedState(onSignInSuccess)
    val client=remember {
        GoogleSignIn.getClient(context,GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id)).requestEmail().build())
    }
    fun failure(message: String) { busy=false; Toast.makeText(context,message,Toast.LENGTH_LONG).show() }
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        try {
            val token=GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(ApiException::class.java).idToken
            if (token == null) failure("Không nhận được thông tin đăng nhập Google. Vui lòng thử lại.")
            else FirebaseAuth.getInstance().signInWithCredential(GoogleAuthProvider.getCredential(token,null)).addOnCompleteListener { task ->
                val user=FirebaseAuth.getInstance().currentUser
                if (!task.isSuccessful || user?.email == null) failure("Đăng nhập Google thất bại. Vui lòng thử lại.")
                else if (!user.isEmailVerified) { FirebaseAuth.getInstance().signOut(); failure("Cần xác minh email trước khi đăng nhập.") }
                else { busy=false; callback(user.email!!) }
            }
        } catch (_: Exception) { failure("Không hoàn tất đăng nhập Google. Kiểm tra cấu hình và thử lại.") }
    }
    Button(onClick={ busy=true; launcher.launch(client.signInIntent) },enabled=!busy,
        modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Color.Red.copy(alpha=0.6f))) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Image(painterResource(R.drawable.gg_logo),contentDescription="Google",modifier=Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp)); Text(if(busy) "Đang đăng nhập…" else "Đăng nhập bằng Google")
        }
    }
}
