package com.example.datban.service;

import com.example.datban.model.User;
import com.example.datban.repository.UserRepository;
import com.example.datban.security.*;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import java.util.List;

@Service
public class AuthService {
    private final UserRepository users;
    private final TokenVerifier verifier;
    private final JdbcTemplate db;
    public AuthService(UserRepository users, TokenVerifier verifier, JdbcTemplate db) {
        this.users = users; this.verifier = verifier; this.db = db;
    }
    @Transactional
    public User synchronizeUser(String token) throws Exception { return synchronize(verifier.verify(token)); }

    @Transactional
    public RestaurantPrincipal authenticate(String token) throws Exception {
        var user = synchronize(verifier.verify(token));
        return new RestaurantPrincipal(user.getUid(), user.getEmail(), roles(user.getUid()));
    }
    private User synchronize(TokenVerifier.Identity identity) {
        if (identity.uid() == null || identity.email() == null || !identity.emailVerified()) {
            throw new AccessDeniedException("Cần xác minh email trước khi sử dụng dịch vụ");
        }
        // Serialize account provisioning. Never infer ownership or privileges from client-supplied email/roles.
        db.queryForList("SELECT id FROM roles WHERE code = 'CUSTOMER' FOR UPDATE");
        var user = users.findById(identity.uid()).orElseGet(User::new);
        if (user.getUid() != null) {
            String status = db.queryForObject("SELECT status FROM users WHERE uid = ?", String.class, identity.uid());
            if (!"ACTIVE".equals(status)) throw new AccessDeniedException("Tài khoản đã bị khóa");
        }
        String email = identity.email().toLowerCase(java.util.Locale.ROOT);
        users.findByEmail(email).ifPresent(existing -> {
            if (!existing.getUid().equals(identity.uid())) throw new AccessDeniedException("Email đã liên kết tài khoản khác");
        });
        user.setUid(identity.uid()); user.setEmail(email);
        String name = identity.name() == null ? email : identity.name();
        user.setTen(name.substring(0,Math.min(name.length(),255)));
        user.setFirebaseProvider(identity.provider() == null ? "unknown" : identity.provider());
        users.saveAndFlush(user);
        if (roles(user.getUid()).isEmpty()) db.update("INSERT INTO user_roles(user_uid, role_id) SELECT ?, id FROM roles WHERE code = 'CUSTOMER'", user.getUid());
        return user;
    }
    public List<String> roles(String uid) {
        return db.queryForList("SELECT r.code FROM roles r JOIN user_roles ur ON ur.role_id = r.id WHERE ur.user_uid = ? ORDER BY r.code", String.class, uid);
    }
}
