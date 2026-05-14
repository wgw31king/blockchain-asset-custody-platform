package io.github.wahhh.bacp.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Refresh token rotation payload.
 */
@Data
@Schema(description = "Refresh token request")
public class RefreshTokenRequest {

    @NotBlank
    @Schema(
            description = "Previously issued refresh JWT",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;
}
