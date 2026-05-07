package io.github.wahhh.bacp.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * OAuth-style token bundle returned after authentication.
 */
@Data
@Builder
@Schema(description = "Issued JWT tokens")
public class LoginResponse {

    @Schema(description = "Bearer access token")
    private String accessToken;

    @Schema(description = "Opaque refresh token")
    private String refreshToken;

    @Schema(description = "Access token TTL seconds")
    private long expiresIn;
}
