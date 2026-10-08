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
    // Monitoring credentials never provision a Firebase user or grant business permissions.
    @Bean
    @org.springframework.core.annotation.Order(1)
    public SecurityFilterChain monitoringChain(HttpSecurity http,
            @Value("${app.monitoring.token:}") String token) throws Exception {
        http.securityMatcher("/actuator/**")
            .csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(f -> f.disable()).httpBasic(b -> b.disable())
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/prometheus").access((authentication, context) -> {
                    String header = context.getRequest().getHeader("Authorization");
                    boolean granted = token.length() >= 32 && header != null && header.startsWith("Bearer ")
                        && java.security.MessageDigest.isEqual(token.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                            header.substring(7).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    return new org.springframework.security.authorization.AuthorizationDecision(granted);
                }).anyRequest().denyAll())
            .exceptionHandling(e -> e.authenticationEntryPoint((q,r,ex) -> FirebaseAuthenticationFilter.failure(r,401,"UNAUTHENTICATED",q.getRequestURI()))
                .accessDeniedHandler((q,r,ex) -> FirebaseAuthenticationFilter.failure(r,403,"ACCESS_DENIED",q.getRequestURI())));
        return http.build();
    }
    @Bean
    @org.springframework.core.annotation.Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthService auth,
            com.example.datban.security.RequestRateLimiter limiter,
            @Value("${app.security.require-auth:false}") boolean requireAuth) throws Exception {
        // Production uses explicit bearer tokens, not cookie/session authentication.
        http.csrf(csrf -> csrf.disable()).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(f -> f.disable()).httpBasic(b -> b.disable())
            .exceptionHandling(e -> e.authenticationEntryPoint((q, r, ex) -> FirebaseAuthenticationFilter.failure(r, 401, "UNAUTHENTICATED",q.getRequestURI()))
                    .accessDeniedHandler((q, r, ex) -> FirebaseAuthenticationFilter.failure(r, 403, "ACCESS_DENIED",q.getRequestURI())))
            .authorizeHttpRequests(a -> {
                a.requestMatchers("/api/admin/**").hasRole("ADMIN")
                 .requestMatchers(HttpMethod.POST, "/nhahang/**").hasRole("ADMIN")
                 .requestMatchers("/api/staff/**", "/nhahang/**").hasAnyRole("STAFF", "ADMIN")
                 .requestMatchers("/api/payments/**", "/api/notifications/**", "/api/devices/**", "/api/taikhoan/**", "/api/dondat/**", "/api/yeu-thich/**", "/api/binhluan/**").authenticated()
                 .requestMatchers(HttpMethod.GET, "/api/thucdon/**", "/api/ban-slot", "/actuator/health", "/uploads/**", "/staff.html", "/staff.js", "/staff.css", "/demo.html", "/demo.js", "/demo.css", "/api/demo/config", "/api/auth/web-config").permitAll()
                 .requestMatchers(HttpMethod.POST, "/api/demo/session").permitAll()
                 .requestMatchers(HttpMethod.POST, "/api/auth/sync").permitAll();
                if (requireAuth) a.anyRequest().authenticated(); else a.anyRequest().permitAll();
            }).headers(h -> h.contentSecurityPolicy(c -> c.policyDirectives("default-src 'self'; connect-src 'self' https://identitytoolkit.googleapis.com; img-src 'self' data:; frame-ancestors 'none'; base-uri 'self'; form-action 'self'")))
            .addFilterBefore(new FirebaseAuthenticationFilter(auth), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new com.example.datban.security.RateLimitFilter(limiter), FirebaseAuthenticationFilter.class);
        return http.build();
    }
}
