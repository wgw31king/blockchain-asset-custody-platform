package io.github.wahhh.bacp.controller.auth;

import io.github.wahhh.bacp.common.constant.SecurityConstants;
import io.github.wahhh.bacp.common.result.Result;
import io.github.wahhh.bacp.common.web.SecurityHelper;
import io.github.wahhh.bacp.config.openapi.OpenApiExamples;
import io.github.wahhh.bacp.dto.request.LoginRequest;
import io.github.wahhh.bacp.dto.request.RefreshTokenRequest;
import io.github.wahhh.bacp.dto.response.LoginResponse;
import io.github.wahhh.bacp.entity.SysUser;
import io.github.wahhh.bacp.risk.ratelimit.RateLimit;
import io.github.wahhh.bacp.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stateless JWT authentication endpoints.
 */
@Tag(
        name = "Auth",
        description = "Login, refresh, logout, and profile. Login is rate-limited; locked accounts return HTTP 200 "
                + "with `code` 403 in the body.")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @SecurityRequirements
    @Operation(
            summary = "Login with username/password",
            description = "Issues access and refresh JWTs. Does not require `Authorization`. Response uses HTTP 200 on "
                    + "success; repeated failures may yield HTTP 429 with `code` 4003.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Success — tokens in `data`",
                    content =
                            @Content(
                                    mediaType = "application/json",
                                    examples =
                                            @ExampleObject(
                                                    name = "Success",
                                                    value = OpenApiExamples.RES_OK_LOGIN))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bean validation failed",
                    content =
                            @Content(
                                    mediaType = "application/json",
                                    examples =
                                            @ExampleObject(
                                                    name = "Validation",
                                                    value = OpenApiExamples.RES_VALIDATION))),
            @ApiResponse(
                    responseCode = "429",
                    description = "Login rate limit exceeded",
                    content =
                            @Content(
                                    mediaType = "application/json",
                                    examples =
                                            @ExampleObject(
                                                    name = "RateLimited",
                                                    value = OpenApiExamples.RES_RATE_LIMITED)))
    })
    @RateLimit(capacity = 30, refillPerSec = 1, cost = 1)
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @SecurityRequirements
    @Operation(
            summary = "Refresh access token",
            description = "Rotates access token using refresh JWT in body. Does not require `Authorization` header.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "New token bundle",
                    content =
                            @Content(
                                    mediaType = "application/json",
                                    examples =
                                            @ExampleObject(name = "Success", value = OpenApiExamples.RES_OK_LOGIN))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error",
                    content =
                            @Content(
                                    mediaType = "application/json",
                                    examples =
                                            @ExampleObject(
                                                    name = "Validation",
                                                    value = OpenApiExamples.RES_VALIDATION)))
    })
    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return Result.ok(authService.refresh(request));
    }

    @Operation(
            summary = "Logout current session",
            description = "Blacklists the presented access token in Redis. Requires `Authorization: Bearer`.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Logged out",
                    content =
                            @Content(
                                    mediaType = "application/json",
                                    examples =
                                            @ExampleObject(name = "Ok", value = OpenApiExamples.RES_OK_VOID))),
            @ApiResponse(
                    responseCode = "401",
                    description = "Missing or invalid bearer token",
                    content =
                            @Content(
                                    mediaType = "application/json",
                                    examples =
                                            @ExampleObject(
                                                    name = "Unauthorized",
                                                    value = OpenApiExamples.RES_UNAUTHORIZED)))
    })
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        authService.logout(request.getHeader(SecurityConstants.AUTH_HEADER));
        return Result.ok();
    }

    @Operation(summary = "Current user profile", description = "Returns `SysUser` without password hash.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Profile payload",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated",
                    content =
                            @Content(
                                    mediaType = "application/json",
                                    examples =
                                            @ExampleObject(
                                                    name = "Unauthorized",
                                                    value = OpenApiExamples.RES_UNAUTHORIZED)))
    })
    @GetMapping("/me")
    public Result<SysUser> me() {
        Long uid = SecurityHelper.currentUserIdOrThrow();
        return Result.ok(authService.profile(uid));
    }
}
