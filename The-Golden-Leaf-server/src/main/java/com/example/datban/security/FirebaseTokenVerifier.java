package com.example.datban.security;

import com.google.firebase.auth.FirebaseAuth;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class FirebaseTokenVerifier implements TokenVerifier {
    public Identity verify(String token) throws Exception {
        if (token == null || token.isBlank() || token.length() > 16384) throw new IllegalArgumentException("Invalid token");
        var decoded = FirebaseAuth.getInstance().verifyIdToken(token, true);
        var firebase = decoded.getClaims().get("firebase");
        String provider = firebase instanceof Map<?, ?> claims
                ? String.valueOf(claims.get("sign_in_provider")) : "unknown";
        return new Identity(decoded.getUid(), decoded.getEmail(), decoded.getName(), provider, decoded.isEmailVerified());
    }
}
