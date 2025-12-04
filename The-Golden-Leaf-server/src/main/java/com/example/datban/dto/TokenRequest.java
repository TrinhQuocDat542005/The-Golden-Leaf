package com.example.datban.dto;

public class TokenRequest {
    private String idToken;

    // Constructors
    public TokenRequest() {}
    public TokenRequest(String idToken) {
        this.idToken = idToken;
    }

    // Getter and Setter
    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
}