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
                        .description("""
                                BACP REST API — JWT authentication, RBAC, custody, demo spot matching, risk, and ops \
                                visibility.

                                **Response envelope:** JSON bodies use `Result<T>` (`code`, `message`, `data`, \
                                `timestamp`). Paginated admin lists use `PageResult<T>` inside `data`.

                                **HTTP status vs business codes:** `BizException` is returned with **HTTP 200** and a \
                                non-success `code` in the JSON body. Framework mapping uses real HTTP statuses for \
                                validation failures (**400**), missing/invalid auth (**401**), `@PreAuthorize` denial \
                                (**403**), rate limits (**429**), and uncaught errors (**500**).

                                **Admin routes:** `/api/v1/admin/**` require both JWT authorities and client IP to match \
                                `bacp.security.admin-ip-whitelist`.

                                **Authentication:** send `Authorization: Bearer <accessToken>` except on `/api/v1/auth/login` \
                                and `/api/v1/auth/refresh`.
                                """)
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
