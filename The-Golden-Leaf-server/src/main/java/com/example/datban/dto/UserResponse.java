package com.example.datban.dto;

public class UserResponse {
    private String uid;
    private String email;
    private String ten;
    private String firebaseProvider;

    public UserResponse(String uid, String email, String ten, String firebaseProvider) {
        this.uid = uid;
        this.email = email;
        this.ten = ten;
        this.firebaseProvider = firebaseProvider;
    }

    // Getters
    public String getUid() { return uid; }
    public String getEmail() { return email; }
    public String getTen() { return ten; }
    public String getFirebaseProvider() { return firebaseProvider; }
    
    // Setters (Nếu cần)
    public void setUid(String uid) { this.uid = uid; }
    public void setEmail(String email) { this.email = email; }
    public void setTen(String ten) { this.ten = ten; }
    public void setFirebaseProvider(String firebaseProvider) { this.firebaseProvider = firebaseProvider; }
}