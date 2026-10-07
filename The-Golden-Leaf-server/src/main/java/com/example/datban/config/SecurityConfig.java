package com.example.datban.config;

import com.example.datban.service.AuthService;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthService auth,
            @Value("${app.security.require-auth:false}") boolean requireAuth) throws Exception {
        // No cookies, sessions or browser credentials: all mutations require an explicit bearer token.
        http.csrf(csrf -> csrf.disable()).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(f -> f.disable()).httpBasic(b -> b.disable())
            .exceptionHandling(e -> e.authenticationEntryPoint((q, r, ex) -> FirebaseAuthenticationFilter.failure(r, 401, "UNAUTHENTICATED",q.getRequestURI()))
                    .accessDeniedHandler((q, r, ex) -> FirebaseAuthenticationFilter.failure(r, 403, "ACCESS_DENIED",q.getRequestURI())))
            .authorizeHttpRequests(a -> {
                a.requestMatchers("/api/admin/**").hasRole("ADMIN")
                 .requestMatchers(HttpMethod.POST, "/nhahang/**").hasRole("ADMIN")
                 .requestMatchers("/api/staff/**", "/nhahang/**").hasAnyRole("STAFF", "ADMIN")
                 .requestMatchers("/api/payments/**", "/api/notifications/**", "/api/devices/**", "/api/taikhoan/**", "/api/dondat/**").authenticated()
                 .requestMatchers(HttpMethod.GET, "/api/thucdon/**", "/api/ban-slot", "/actuator/health", "/uploads/**", "/staff.html", "/staff.js", "/staff.css", "/api/auth/web-config").permitAll()
                 .requestMatchers(HttpMethod.POST, "/api/auth/sync").permitAll();
                if (requireAuth) a.anyRequest().authenticated(); else a.anyRequest().permitAll();
            }).headers(h -> h.contentSecurityPolicy(c -> c.policyDirectives("default-src 'self'; connect-src 'self' https://identitytoolkit.googleapis.com; img-src 'self' data:; frame-ancestors 'none'; base-uri 'self'; form-action 'self'")))
            .addFilterBefore(new FirebaseAuthenticationFilter(auth), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
