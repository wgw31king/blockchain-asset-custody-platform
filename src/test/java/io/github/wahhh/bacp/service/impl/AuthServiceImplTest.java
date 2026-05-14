package io.github.wahhh.bacp.service.impl;

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
import io.github.wahhh.bacp.entity.SysUser;
import io.github.wahhh.bacp.mapper.SysPermissionMapper;
import io.github.wahhh.bacp.mapper.SysRoleMapper;
import io.github.wahhh.bacp.mapper.SysUserMapper;
import io.github.wahhh.bacp.monitor.metrics.UserActivityMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.ObjectProvider;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String SECRET = "test-jwt-secret-must-be-at-least-32-bytes-long!!";

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private SysPermissionMapper sysPermissionMapper;

    @Mock
    private SysRoleMapper sysRoleMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private ObjectProvider<UserActivityMetrics> userActivityMetrics;

    private final JwtUtil jwtUtil = new JwtUtil(SECRET, "bacp-test", "bacp-api-test");

    private final BacpSecurityProperties securityProperties = new BacpSecurityProperties();

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        securityProperties.getJwt().setAccessTokenTtlSeconds(3600);
        securityProperties.getJwt().setRefreshTokenTtlSeconds(604800);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(userActivityMetrics.getIfAvailable()).thenReturn(null);
        authService = new AuthServiceImpl(sysUserMapper, sysPermissionMapper, sysRoleMapper,
                passwordEncoder, jwtUtil, securityProperties, stringRedisTemplate, tokenBlacklistService,
                meterRegistry, userActivityMetrics);
    }

    @Test
    void loginSuccess() {
        when(valueOperations.get(anyString())).thenReturn(null);
        SysUser user = new SysUser();
        user.setId(9L);
        user.setUsername("alice");
        user.setPasswordHash("hash");
        user.setStatus(1);
        when(sysUserMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches(eq("pw"), eq("hash"))).thenReturn(true);
        when(sysPermissionMapper.selectPermCodesByUserId(9L)).thenReturn(List.of("trade:place"));
        when(sysRoleMapper.selectRoleCodesByUserId(9L)).thenReturn(List.of("USER"));

        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword("pw");

        LoginResponse resp = authService.login(req);
        assertNotNull(resp.getAccessToken());
        verify(stringRedisTemplate).delete(anyString());
    }

    @Test
    void loginFailsBadPassword() {
        when(valueOperations.get(anyString())).thenReturn(null);
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("bob");
        user.setPasswordHash("hash");
        user.setStatus(1);
        when(sysUserMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(stringRedisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);

        LoginRequest req = new LoginRequest();
        req.setUsername("bob");
        req.setPassword("wrong");

        assertThrows(AuthException.class, () -> authService.login(req));
    }

    @Test
    void refreshRotatesTokens() {
        String refresh = jwtUtil.generateRefresh(3L, "carol", 7200);
        when(sysPermissionMapper.selectPermCodesByUserId(3L)).thenReturn(List.of());
        when(sysRoleMapper.selectRoleCodesByUserId(3L)).thenReturn(List.of());

        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken(refresh);

        LoginResponse out = authService.refresh(req);
        assertNotNull(out.getAccessToken());
    }

    @Test
    void logoutBlacklistsJti() {
        String access = jwtUtil.generate(5L, "dave", List.of("x"), 600);
        authService.logout(SecurityConstants.BEARER_PREFIX + access);
        verify(tokenBlacklistService).blacklist(anyString(), anyLong());
    }

    @Test
    void logoutIgnoresMalformedHeader() {
        authService.logout("not-bearer");
        verify(tokenBlacklistService, never()).blacklist(anyString(), anyLong());
    }

    @Test
    void profileScrubsPassword() {
        SysUser user = new SysUser();
        user.setId(8L);
        user.setUsername("ex");
        user.setPasswordHash("secret-hash");
        when(sysUserMapper.selectById(8L)).thenReturn(user);

        SysUser out = authService.profile(8L);
        assertNull(out.getPasswordHash());
    }

    @Test
    void profileMissingThrows() {
        when(sysUserMapper.selectById(99L)).thenReturn(null);
        assertThrows(BizException.class, () -> authService.profile(99L));
    }

    @Test
    void loginLockedAfterFailures() {
        when(valueOperations.get(anyString())).thenReturn(String.valueOf(securityProperties.getLogin().getMaxFailCount()));

        LoginRequest req = new LoginRequest();
        req.setUsername("locked");
        req.setPassword("x");

        BizException ex = assertThrows(BizException.class, () -> authService.login(req));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }
}
