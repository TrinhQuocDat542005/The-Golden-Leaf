package com.example.datban.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id; // Không dùng GeneratedValue nữa

@Entity
public class User {

    // 💡 THAY ĐỔI 1: Sử dụng Firebase UID (String) làm khóa chính @Id
    @Id
    private String uid; 

    private String email;
    private String ten;
    
    // 💡 THAY ĐỔI 2: Thêm trường để lưu Provider ID (vd: "google.com", "password")
    private String firebaseProvider; 

    // Constructor rỗng (cần thiết cho JPA)
    public User() {}

    // Getter và Setter
    // Thay thế getId/setId bằng getUid/setUid
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTen() { return ten; }
    public void setTen(String ten) { this.ten = ten; }

    // Getter và Setter mới cho firebaseProvider
    public String getFirebaseProvider() { return firebaseProvider; }
    public void setFirebaseProvider(String firebaseProvider) { this.firebaseProvider = firebaseProvider; }
}