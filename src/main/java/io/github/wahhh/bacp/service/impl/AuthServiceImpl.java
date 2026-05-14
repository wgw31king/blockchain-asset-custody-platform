package io.github.wahhh.bacp.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.wahhh.bacp.common.constant.SecurityConstants;
import io.github.wahhh.bacp.common.exception.AuthException;
import io.github.wahhh.bacp.common.exception.BizException;
import io.github.wahhh.bacp.common.result.ResultCode;
import io.github.wahhh.bacp.common.util.JwtUtil;
import io.github.wahhh.bacp.config.properties.BacpSecurityProperties;
import io.github.wahhh.bacp.config.security.TokenBlacklistService;
import io.github.wahhh.bacp.dto.request.LoginRequest;
import io.github.wahhh.bacp.dto.request.RefreshTokenRequest;
import io.github.wahhh.bacp.dto.response.LoginResponse;
import io.github.wahhh.bacp.entity.SysPermission;
import io.github.wahhh.bacp.entity.SysUser;
import io.github.wahhh.bacp.mapper.SysPermissionMapper;
import io.github.wahhh.bacp.mapper.SysRoleMapper;
import io.github.wahhh.bacp.mapper.SysUserMapper;
import io.github.wahhh.bacp.monitor.metrics.UserActivityMetrics;
import io.github.wahhh.bacp.service.AuthService;
import io.jsonwebtoken.Claims;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Default {@link AuthService} backed by database roles and Redis safeguards.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;

    private final SysPermissionMapper sysPermissionMapper;

    private final SysRoleMapper sysRoleMapper;

    private final PasswordEncoder passwordEncoder;

    private final JwtUtil jwtUtil;

    private final BacpSecurityProperties securityProperties;

    private final StringRedisTemplate stringRedisTemplate;

    private final TokenBlacklistService tokenBlacklistService;

    private final MeterRegistry meterRegistry;

    private final ObjectProvider<UserActivityMetrics> userActivityMetrics;

    /**
     * {@inheritDoc}
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        String lockKey = SecurityConstants.LOGIN_FAIL_KEY + request.getUsername();
        String fails = stringRedisTemplate.opsForValue().get(lockKey);
        int failCount = fails == null ? 0 : Integer.parseInt(fails);
        if (failCount >= securityProperties.getLogin().getMaxFailCount()) {
            throw new BizException(ResultCode.FORBIDDEN, "account temporarily locked");
        }
        SysUser user = sysUserMapper.selectOne(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, request.getUsername()));
        if (user == null || user.getStatus() != null && user.getStatus() == 0) {
            bumpFail(lockKey);
            meterRegistry.counter("bacp_login_total", "result", "failure").increment();
            throw new AuthException("invalid credentials");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            bumpFail(lockKey);
            meterRegistry.counter("bacp_login_total", "result", "failure").increment();
            throw new AuthException("invalid credentials");
        }
        stringRedisTemplate.delete(lockKey);
        Set<String> perms = new HashSet<>(loadPermissions(user.getId()));
        List<String> roles = sysRoleMapper.selectRoleCodesByUserId(user.getId());
        if (roles.stream().anyMatch("SUPER_ADMIN"::equalsIgnoreCase)) {
            sysPermissionMapper.selectList(Wrappers.<SysPermission>lambdaQuery())
                    .stream()
                    .map(SysPermission::getPermCode)
                    .forEach(perms::add);
            perms.add(SecurityConstants.PERM_ALL);
        }
        long accessTtl = securityProperties.getJwt().getAccessTokenTtlSeconds();
        long refreshTtl = securityProperties.getJwt().getRefreshTokenTtlSeconds();
        String access = jwtUtil.generate(user.getId(), user.getUsername(), perms, accessTtl);
        String refresh = jwtUtil.generateRefresh(user.getId(), user.getUsername(), refreshTtl);
        meterRegistry.counter("bacp_login_total", "result", "success").increment();
        userActivityMetrics.ifAvailable(m -> m.recordSuccessfulLogin(user.getId()));
        return LoginResponse.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .expiresIn(accessTtl)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LoginResponse refresh(RefreshTokenRequest request) {
        Claims claims = jwtUtil.parse(request.getRefreshToken());
        if (!"refresh".equalsIgnoreCase(String.valueOf(claims.get("typ")))) {
            throw new AuthException("invalid refresh token");
        }
        Long uid = claims.get("uid", Long.class);
        String username = claims.getSubject();
        Set<String> perms = new HashSet<>(loadPermissions(uid));
        List<String> roles = sysRoleMapper.selectRoleCodesByUserId(uid);
        if (roles.stream().anyMatch("SUPER_ADMIN"::equalsIgnoreCase)) {
            sysPermissionMapper.selectList(Wrappers.<SysPermission>lambdaQuery())
                    .stream()
                    .map(SysPermission::getPermCode)
                    .forEach(perms::add);
            perms.add(SecurityConstants.PERM_ALL);
        }
        long accessTtl = securityProperties.getJwt().getAccessTokenTtlSeconds();
        long refreshTtl = securityProperties.getJwt().getRefreshTokenTtlSeconds();
        String access = jwtUtil.generate(uid, username, perms, accessTtl);
        String refresh = jwtUtil.generateRefresh(uid, username, refreshTtl);
        return LoginResponse.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .expiresIn(accessTtl)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void logout(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith(SecurityConstants.BEARER_PREFIX)) {
            return;
        }
        String token = bearerToken.substring(SecurityConstants.BEARER_PREFIX.length()).trim();
        Claims claims = jwtUtil.parse(token);
        String jti = claims.get("jti", String.class);
        long ttl = Math.max(jwtUtil.getRemainingSeconds(token), 1L);
        tokenBlacklistService.blacklist(jti, ttl);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SysUser profile(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND, "user not found");
        }
        user.setPasswordHash(null);
        return user;
    }

    private List<String> loadPermissions(Long userId) {
        return sysPermissionMapper.selectPermCodesByUserId(userId);
    }

    private void bumpFail(String lockKey) {
        Long newVal = stringRedisTemplate.opsForValue().increment(lockKey);
        Duration ttl = Duration.ofMinutes(Math.max(securityProperties.getLogin().getLockMinutes(), 1));
        stringRedisTemplate.expire(lockKey, ttl);
        if (newVal != null && newVal >= securityProperties.getLogin().getMaxFailCount()) {
            log.warn("Login lock engaged for {}", lockKey);
        }
    }
}
