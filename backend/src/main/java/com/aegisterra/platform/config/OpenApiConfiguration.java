package com.aegisterra.platform.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI aegisTerraOpenApi(@Value("${aegisterra.version:1.0.0}") String version) {
        final String bearer = "bearerAuth";
        return new OpenAPI()
            .info(new Info()
                .title("AegisTerra Platform API")
                .description("""
                    National agricultural insurance platform API.

                    JWT access + opaque refresh tokens via HttpOnly cookies (also Bearer for API clients).
                    """)
                .version(version)
                .license(new License().name("Proprietary")))
            .addSecurityItem(new SecurityRequirement().addList(bearer))
            .components(new Components().addSecuritySchemes(bearer,
                new SecurityScheme()
                    .name(bearer)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
}
