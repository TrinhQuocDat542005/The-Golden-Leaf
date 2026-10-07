package com.example.datban.config;

import com.example.datban.service.AuthService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

public class FirebaseAuthenticationFilter extends OncePerRequestFilter {
    private final AuthService auth;
    public FirebaseAuthenticationFilter(AuthService auth) { this.auth = auth; }
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null) {
            if (!header.startsWith("Bearer ")) { failure(response, 401, "UNAUTHENTICATED",request.getRequestURI()); return; }
            try {
                var principal = auth.authenticate(header.substring(7));
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                        principal, null, principal.roles().stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList()));
            } catch (org.springframework.security.access.AccessDeniedException ex) {
                failure(response, 403, "ACCESS_DENIED",request.getRequestURI()); return;
            } catch (org.springframework.dao.DataAccessException | IllegalStateException ex) {
                failure(response, 503, "AUTH_UNAVAILABLE",request.getRequestURI()); return;
            } catch (Exception ex) {
                failure(response, 401, "UNAUTHENTICATED",request.getRequestURI()); return;
            }
        }
        chain.doFilter(request, response);
    }
    public static void failure(HttpServletResponse response, int status, String code,String path) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(status); response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
        new com.fasterxml.jackson.databind.ObjectMapper().writeValue(response.getWriter(),java.util.Map.of(
                "timestamp",java.time.Instant.now().toString(),"status",status,"error",org.springframework.http.HttpStatus.valueOf(status).getReasonPhrase(),
                "code",code,"message","Không có quyền truy cập hoặc phiên đăng nhập không hợp lệ","path",path,"fieldErrors",java.util.Map.of()));
    }
}
