package com.example.datban.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI goldenLeafOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("The Golden Leaf API")
                        .version("v1")
                        .description("API đặt bàn, thực đơn và thanh toán của The Golden Leaf"))
                .components(new Components().addSecuritySchemes("firebaseBearer", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("Firebase ID token")));
    }
}
