package io.github.wahhh.bacp.common.util;

import io.github.wahhh.bacp.common.exception.BizException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private static final String SECRET = "test-jwt-secret-must-be-at-least-32-bytes-long!!";

    @Test
    void rejectsShortSecret() {
        assertThrows(IllegalArgumentException.class, () -> new JwtUtil("too-short", "iss", "aud"));
    }

    @Test
    void generateParseAndValidateRefresh() {
        JwtUtil jwt = new JwtUtil(SECRET, "bacp-test", "bacp-api-test");
        String access = jwt.generate(1L, "admin", List.of("user:query"), 3600);
        assertDoesNotThrow(() -> jwt.parse(access));
        assertTrue(jwt.isValid(access));
        assertTrue(jwt.getRemainingSeconds(access) > 0);

        String refresh = jwt.generateRefresh(1L, "admin", 3600);
        assertDoesNotThrow(() -> jwt.parse(refresh));
    }

    @Test
    void invalidTokenFailsParse() {
        JwtUtil jwt = new JwtUtil(SECRET, "bacp-test", "bacp-api-test");
        assertThrows(BizException.class, () -> jwt.parse("not-a-jwt"));
        assertFalse(jwt.isValid("not-a-jwt"));
    }
}
