package com.example.datban.repository;

import com.example.datban.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional; // 💡 Cần import này để trả về Optional

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    // String là kiểu dữ liệu của khóa chính (UID)

    /**
     * 💡 KHẮC PHỤC LỖI CONTROLLER: 
     * Khai báo phương thức này để Spring Data JPA tự động tạo truy vấn
     * tìm kiếm người dùng theo trường 'email'.
     */
    Optional<User> findByEmail(String email);
}