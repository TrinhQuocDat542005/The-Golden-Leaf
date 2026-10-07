package com.example.datban.security;

public interface TokenVerifier {
    Identity verify(String token) throws Exception;
    record Identity(String uid, String email, String name, String provider, boolean emailVerified) {}
}
