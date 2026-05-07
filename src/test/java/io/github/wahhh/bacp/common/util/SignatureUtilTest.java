package io.github.wahhh.bacp.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignatureUtilTest {

    @Test
    void canonicalAndSignVerify() {
        String body = "{\"a\":1}";
        String ts = "1700000000000";
        String nonce = "abc";
        String secret = "hmac-secret";
        String sig = SignatureUtil.sign(body, ts, nonce, secret);
        assertEquals(
                SignatureUtil.sign(body, ts, nonce, secret),
                SignatureUtil.sign(body, ts, nonce, secret));
        assertTrue(SignatureUtil.verify(body, ts, nonce, secret, sig));
        assertFalse(SignatureUtil.verify(body, ts, nonce, secret, "deadbeef"));
        assertFalse(SignatureUtil.verify(body, ts, nonce, secret, null));
    }

    @Test
    void canonicalHandlesNulls() {
        assertEquals("\n\n", SignatureUtil.canonicalPayload(null, null, null));
    }
}
