package io.github.wahhh.bacp.controller.auth;

import io.github.wahhh.bacp.common.constant.SecurityConstants;
import io.github.wahhh.bacp.common.result.Result;
import io.github.wahhh.bacp.common.web.SecurityHelper;
import io.github.wahhh.bacp.dto.request.LoginRequest;
import io.github.wahhh.bacp.dto.request.RefreshTokenRequest;
import io.github.wahhh.bacp.dto.response.LoginResponse;
import io.github.wahhh.bacp.entity.SysUser;
import io.github.wahhh.bacp.risk.ratelimit.RateLimit;
import io.github.wahhh.bacp.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "Auth")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Issues JWT access and refresh tokens.
     *
     * @param request credentials
     * @return token envelope
     */
    @Operation(summary = "Login with username/password")
    @RateLimit(capacity = 30, refillPerSec = 1, cost = 1)
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    /**
     * Rotates access token using refresh JWT.
     *
     * @param request refresh token body
     * @return token envelope
     */
    @Operation(summary = "Refresh access token")
    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return Result.ok(authService.refresh(request));
    }

    /**
     * Revokes active access token via Redis blacklist.
     *
     * @param request servlet request carrying Authorization header
     * @return empty payload
     */
    @Operation(summary = "Logout current session")
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        authService.logout(request.getHeader(SecurityConstants.AUTH_HEADER));
        return Result.ok();
    }

    /**
     * Returns sanitized profile for authenticated principal.
     *
     * @return user profile without password hash
     */
    @Operation(summary = "Current user profile")
    @GetMapping("/me")
    public Result<SysUser> me() {
        Long uid = SecurityHelper.currentUserIdOrThrow();
        return Result.ok(authService.profile(uid));
    }
}
