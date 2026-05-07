package io.github.wahhh.bacp.service;

import io.github.wahhh.bacp.dto.request.LoginRequest;
import io.github.wahhh.bacp.dto.request.RefreshTokenRequest;
import io.github.wahhh.bacp.dto.response.LoginResponse;
import io.github.wahhh.bacp.entity.SysUser;

/**
 * Authentication flows (login / refresh / logout).
 */
public interface AuthService {

    /**
     * Validates credentials and mints JWT pair.
     *
     * @param request login payload
     * @return issued tokens
     */
    LoginResponse login(LoginRequest request);

    /**
     * Rotates access token using {@code typ=refresh} JWT.
     *
     * @param request refresh payload
     * @return new access token bundle
     */
    LoginResponse refresh(RefreshTokenRequest request);

    /**
     * Revokes current access token {@code jti}.
     *
     * @param bearerToken Authorization header value including Bearer prefix
     */
    void logout(String bearerToken);

    /**
     * Loads persisted profile for authenticated principal.
     *
     * @param userId user id
     * @return user row
     */
    SysUser profile(Long userId);
}
