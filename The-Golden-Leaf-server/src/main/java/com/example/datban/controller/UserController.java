package com.example.datban.controller;

import com.example.datban.dto.TokenRequest; // 💡 Import DTO Request
import com.example.datban.dto.UserResponse; // 💡 Import DTO Response
import com.example.datban.model.User;
import com.example.datban.service.AuthService; // 💡 Import Service
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    // 💡 KHẮC PHỤC: Sử dụng AuthService thay vì gọi UserRepository trực tiếp
    private final AuthService authService; 

    @Autowired
    public UserController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Endpoint chính để đồng bộ hóa và xác thực người dùng sau khi đăng nhập (Google, Email/Password, etc.).
     * Nhận Firebase ID Token và trả về thông tin người dùng đã được đồng bộ hóa.
     */
    @PostMapping("/sync") // 💡 Đổi tên endpoint cho rõ ràng hơn (sync thay vì google)
    public ResponseEntity<?> synchronizeUser(@RequestBody TokenRequest tokenRequest) {
        try {
            // 1. Chuyển giao toàn bộ trách nhiệm xác thực và đồng bộ cho AuthService
            User user = authService.synchronizeUser(tokenRequest.getIdToken());

            // 2. Chuyển đổi User Model thành UserResponse DTO
            UserResponse response = new UserResponse(
                user.getUid(),
                user.getEmail(),
                user.getTen(),
                user.getFirebaseProvider()
            );

            // 3. Trả về thông tin người dùng thành công (HTTP 200 OK)
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // Log lỗi chi tiết trên server
            e.printStackTrace();
            
            // 4. Trả về lỗi nếu Token không hợp lệ hoặc có vấn đề đồng bộ
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED) // HTTP 401
                .body("Authentication failed or synchronization error: " + e.getMessage());
        }
    }
}