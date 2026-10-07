package com.example.datban.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.beans.factory.annotation.Value;
@Configuration
public class SecurityConfig {

    @Value("${app.security.require-auth:false}")
    private boolean requireAuthentication;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable());

        if (requireAuthentication) {
            http.authorizeHttpRequests(auth -> auth
                    .requestMatchers("/actuator/health", "/uploads/**", "/api/auth/**", "/api/thucdon/**").permitAll()
                    .requestMatchers("/nhahang/**").permitAll()
                    .anyRequest().authenticated())
                .addFilterBefore(
                    new FirebaseAuthenticationFilter(),
                    org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
        } else {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        }

        http.formLogin(form -> form.disable());

        return http.build();
    }
}
