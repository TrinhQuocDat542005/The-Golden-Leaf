package com.example.datban.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final org.springframework.core.env.Environment environment;
    public WebConfig(org.springframework.core.env.Environment environment) { this.environment = environment; }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (environment.matchesProfiles("demo")) return;
        // ánh xạ đường dẫn /uploads/** tới thư mục thật "uploads" ngoài project
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
