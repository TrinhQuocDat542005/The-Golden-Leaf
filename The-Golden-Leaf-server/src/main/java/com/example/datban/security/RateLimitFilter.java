package com.example.datban.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;
import com.example.datban.config.FirebaseAuthenticationFilter;

public class RateLimitFilter extends OncePerRequestFilter {
    private final RequestRateLimiter limiter;
    public RateLimitFilter(RequestRateLimiter limiter) { this.limiter = limiter; }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // Never trust X-Forwarded-For / X-Real-IP from arbitrary callers.
        if (!limiter.allow(request.getRemoteAddr())) {
            response.setHeader("Retry-After", Integer.toString(limiter.retryAfterSeconds()));
            FirebaseAuthenticationFilter.failure(response, 429, "RATE_LIMITED", request.getRequestURI());
            return;
        }
        chain.doFilter(request, response);
    }
}
