package io.github.wahhh.bacp.config.security;

import io.github.wahhh.bacp.common.constant.SecurityConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-backed JWT revocation list using {@code jti} claim.
 */
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Revokes a token until its natural expiry.
     *
     * @param jti           JWT id claim
     * @param ttlSeconds    TTL aligned with remaining lifetime
     */
    public void blacklist(String jti, long ttlSeconds) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        long ttl = Math.max(ttlSeconds, 1L);
        stringRedisTemplate.opsForValue().set(SecurityConstants.JWT_BLACKLIST_KEY + jti, "1", Duration.ofSeconds(ttl));
    }

    /**
     * Checks whether {@code jti} was revoked.
     *
     * @param jti JWT id claim
     * @return true if blacklisted
     */
    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        Boolean exists = stringRedisTemplate.hasKey(SecurityConstants.JWT_BLACKLIST_KEY + jti);
        return Boolean.TRUE.equals(exists);
    }
}
