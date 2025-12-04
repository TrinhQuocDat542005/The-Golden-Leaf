package com.example.datban.service;

import com.example.datban.model.User;
import com.example.datban.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Đồng bộ hóa thông tin người dùng từ Firebase sang DB quan hệ.
     * @param idToken Firebase ID Token gửi từ Client
     * @return User object đã được lưu/cập nhật
     * @throws Exception nếu Token không hợp lệ
     */
    public User synchronizeUser(String idToken) throws Exception {
        
        // 1. Giải mã Firebase ID Token
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
        
        String uid = decodedToken.getUid();
        String email = decodedToken.getEmail();
        String name = decodedToken.getName(); 
        
        // 💡 KHẮC PHỤC LỖI [36,41]: Lấy provider từ Claims vì decodedToken.getSignInProvider() không tồn tại.
        // Thông tin nhà cung cấp được lưu trữ trong claims dưới dạng "firebase.sign_in_provider"
        String providerId = (String) decodedToken.getClaims().get("firebase_sign_in_provider"); 
        
        // Lưu ý: Tùy thuộc vào phiên bản Firebase SDK, đôi khi nó nằm trong .get("firebase").get("sign_in_provider")
        // Tuy nhiên, cách truy cập trực tiếp claims.get("firebase_sign_in_provider") là phổ biến nhất.

        // 2. Kiểm tra sự tồn tại của người dùng trong DB (Dựa trên UID)
        Optional<User> existingUser = userRepository.findById(uid);
        User user;

        if (existingUser.isPresent()) {
            // 3a. Người dùng ĐÃ tồn tại -> Cập nhật thông tin
            user = existingUser.get();
            user.setEmail(email); 
            if (name != null) user.setTen(name);
            // Cập nhật provider (đảm bảo không NULL)
            if (providerId != null) user.setFirebaseProvider(providerId);
            
        } else {
            // 3b. Người dùng CHƯA tồn tại -> Tạo người dùng mới
            user = new User();
            user.setUid(uid); // Đặt UID làm khóa chính
            user.setEmail(email);
            user.setTen(name != null ? name : email); 
            user.setFirebaseProvider(providerId != null ? providerId : "unknown"); // Đảm bảo không NULL
        }

        // 4. Lưu hoặc Cập nhật vào DB
        return userRepository.save(user);
    }
}