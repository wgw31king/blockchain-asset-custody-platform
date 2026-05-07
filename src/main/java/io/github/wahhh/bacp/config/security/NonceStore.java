package io.github.wahhh.bacp.config.security;

import io.github.wahhh.bacp.common.constant.CacheKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Stores request nonces to prevent replay within a TTL window.
 */
@Service
@RequiredArgsConstructor
public class NonceStore {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Atomically records a nonce; returns false if it already exists.
     *
     * @param nonce    unique nonce
     * @param ttlSeconds TTL for storage
     * @return true if nonce was accepted
     */
    public boolean checkAndStore(String nonce, long ttlSeconds) {
        if (nonce == null || nonce.isBlank()) {
            return false;
        }
        String key = CacheKeys.NONCE + nonce;
        Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(Math.max(ttlSeconds, 1L)));
        return Boolean.TRUE.equals(ok);
    }
}
