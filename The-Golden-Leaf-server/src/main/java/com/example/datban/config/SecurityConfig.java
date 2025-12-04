package com.example.datban.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeHttpRequests(auth -> auth
             .requestMatchers("/uploads/**").permitAll() 
                .requestMatchers("/api/giohang/**").permitAll()
                .requestMatchers("/api/datban/**").authenticated()
    

                .requestMatchers("/api/ban-slot/**").authenticated() 
                    .requestMatchers("/api/thucdon/**").authenticated() 
                    .requestMatchers("/nhahang/**").permitAll()
                    .requestMatchers("/api/hoadon/**").authenticated()
            )
            .addFilterBefore(
                new FirebaseAuthenticationFilter(),
                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class
            )
            .formLogin().disable();

        return http.build();
    }
}
