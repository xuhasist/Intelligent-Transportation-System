package com.demo.config;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;

import java.util.List;

@Configuration
public class SwaggerConfig {
    // http://localhost:port/swagger-ui/index.html
    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title("Traffic API")
                        .version("1.0")
                        .description("API docs"))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))    // global setting
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .name("Authorization")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .in(SecurityScheme.In.HEADER)))
                .servers(List.of(
                        new Server().url("http://localhost:8443").description("Local Server"),
                        new Server().url("https://api.demo.com").description("Production server")
                ));
    }
}
