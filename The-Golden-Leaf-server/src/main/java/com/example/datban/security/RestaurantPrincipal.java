package com.example.datban.security;

import java.security.Principal;
import java.util.List;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;

public record RestaurantPrincipal(String uid, String email, List<String> roles) implements Principal {
    public String getName() { return email; }
    public static RestaurantPrincipal current() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof RestaurantPrincipal p ? p : null;
    }
    public static RestaurantPrincipal required() {
        var p = current();
        if (p == null) throw new AccessDeniedException("Cần đăng nhập");
        return p;
    }
    public boolean staff() { return roles.contains("STAFF") || roles.contains("ADMIN"); }
}
