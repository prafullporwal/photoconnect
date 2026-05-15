package com.photoconnect.auth.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the Swagger UI's "Authorize" button to send a Bearer JWT.
 *
 * <p>{@code @SecurityScheme} at class level registers the scheme;
 * {@code @SecurityRequirement(name = "bearerAuth")} on a controller method
 * (or class) advertises that this endpoint expects it.</p>
 */
@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfig {

    @Bean
    public OpenAPI authServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("PhotoConnect — auth-service API")
                .version("v1")
                .description("Registration, login, JWT issuance, refresh, logout."));
    }
}
