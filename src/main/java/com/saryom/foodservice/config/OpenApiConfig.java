package com.saryom.foodservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata for the interactive docs (Swagger UI at {@code /swagger-ui.html},
 * machine-readable spec at {@code /v3/api-docs}). Declares the Firebase bearer-token
 * scheme so the "Authorize" button lets you exercise authenticated endpoints.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearer-jwt";

    @Bean
    OpenAPI foodServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Saryom Food Service API")
                        .description("Surplus/free food sharing: post food, browse nearby, reserve and pick up.")
                        .version("v1"))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Firebase ID token issued to the signed-in user.")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
