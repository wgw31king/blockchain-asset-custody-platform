package io.github.wahhh.bacp.common.util;

import io.github.wahhh.bacp.common.exception.BizException;
import io.github.wahhh.bacp.common.result.ResultCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT creation and parsing utilities using HS256.
 */
@Slf4j
@Component
public class JwtUtil {

    private final SecretKey secretKey;

    private final String issuer;

    private final String audience;

    /**
     * Constructs JWT helper with signing secret and claims metadata.
     *
     * @param secret   symmetric key string (minimum 32 bytes when UTF-8 encoded)
     * @param issuer   JWT issuer claim
     * @param audience JWT audience claim
     */
    public JwtUtil(
            @Value("${bacp.security.jwt.secret}") String secret,
            @Value("${bacp.security.jwt.issuer}") String issuer,
            @Value("${bacp.security.jwt.audience}") String audience) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("bacp.security.jwt.secret must be at least 32 bytes (UTF-8)");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.issuer = issuer;
        this.audience = audience;
    }

    /**
     * Issues an access token embedding user identity and permission codes.
     *
     * @param userId      primary user id
     * @param username    login name
     * @param permissions authority codes (button-level)
     * @param ttlSeconds  time-to-live in seconds
     * @return compact JWT string
     */
    public String generate(Long userId, String username, Collection<String> permissions, long ttlSeconds) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + ttlSeconds * 1000L);
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", userId);
        claims.put("perms", permissions);
        claims.put("jti", UUID.randomUUID().toString());
        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuer(issuer)
                .audience().add(audience).and()
                .issuedAt(now)
                .expiration(exp)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Issues a refresh token with reduced payload (subject only).
     *
     * @param userId     primary user id
     * @param username   login name
     * @param ttlSeconds refresh TTL in seconds
     * @return compact JWT string
     */
    public String generateRefresh(Long userId, String username, long ttlSeconds) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + ttlSeconds * 1000L);
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", userId);
        claims.put("typ", "refresh");
        claims.put("jti", UUID.randomUUID().toString());
        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuer(issuer)
                .audience().add(audience).and()
                .issuedAt(now)
                .expiration(exp)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Parses JWT claims or throws {@link BizException} on invalid token.
     *
     * @param token compact JWT
     * @return parsed claims
     */
    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(issuer)
                    .requireAudience(audience)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            throw new BizException(ResultCode.UNAUTHORIZED, "token expired");
        } catch (JwtException ex) {
            log.debug("JWT parse failed: {}", ex.getMessage());
            throw new BizException(ResultCode.UNAUTHORIZED, "invalid token");
        }
    }

    /**
     * Returns true when token parses without throwing.
     *
     * @param token compact JWT
     * @return validity flag
     */
    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (BizException ex) {
            return false;
        }
    }

    /**
     * Remaining lifetime of token in seconds (negative if expired).
     *
     * @param token compact JWT
     * @return seconds until expiration
     */
    public long getRemainingSeconds(String token) {
        Claims c = parse(token);
        Date exp = c.getExpiration();
        if (exp == null) {
            return 0L;
        }
        return (exp.getTime() - System.currentTimeMillis()) / 1000L;
    }
}
