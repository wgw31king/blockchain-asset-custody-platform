package io.github.wahhh.bacp.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 configuration with bearer JWT security scheme.
 */
@Configuration
public class SwaggerConfig {

    /**
     * Builds OpenAPI document metadata.
     *
     * @return OpenAPI bean
     */
    @Bean
    public OpenAPI bacpOpenApi() {
        final String schemeName = "bearer-jwt";
        return new OpenAPI()
                .info(new Info()
                        .title("Blockchain Asset Custody Platform API")
                        .version("v1")
                        .description("BACP REST API — RBAC, custody, trading, risk, monitoring.")
                        .contact(new Contact().name("wahhh")))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components().addSecuritySchemes(schemeName,
                        new SecurityScheme()
                                .name(schemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
