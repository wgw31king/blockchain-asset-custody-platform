package io.github.wahhh.bacp.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Credential payload for interactive login.
 */
@Data
@Schema(description = "Login request")
public class LoginRequest {

    @NotBlank
    @Schema(description = "Unique username", example = "admin")
    private String username;

    @NotBlank
    @Schema(description = "Plain password", example = "Admin@123")
    private String password;
}
