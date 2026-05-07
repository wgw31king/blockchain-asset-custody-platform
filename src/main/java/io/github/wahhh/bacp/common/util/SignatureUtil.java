package io.github.wahhh.bacp.common.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * HMAC-SHA256 request signing for anti-replay protection.
 */
public final class SignatureUtil {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private SignatureUtil() {
    }

    /**
     * Builds canonical signing payload.
     *
     * @param timestamp epoch millis or ISO string from client
     * @param nonce     unique nonce per request
     * @param body      raw HTTP body (empty string if none)
     * @return canonical string
     */
    public static String canonicalPayload(String timestamp, String nonce, String body) {
        String ts = timestamp == null ? "" : timestamp;
        String n = nonce == null ? "" : nonce;
        String b = body == null ? "" : body;
        return ts + "\n" + n + "\n" + b;
    }

    /**
     * Computes hex-encoded HMAC-SHA256 signature.
     *
     * @param body      raw body
     * @param timestamp client timestamp
     * @param nonce     client nonce
     * @param secret    shared secret
     * @return lowercase hex digest
     */
    public static String sign(String body, String timestamp, String nonce, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] raw = mac.doFinal(canonicalPayload(timestamp, nonce, body).getBytes(StandardCharsets.UTF_8));
            return toHex(raw);
        } catch (Exception ex) {
            throw new IllegalStateException("HMAC sign failed", ex);
        }
    }

    /**
     * Verifies signature using constant-time comparison.
     *
     * @param body             raw body
     * @param timestamp        client timestamp
     * @param nonce            client nonce
     * @param secret           shared secret
     * @param signatureHex     expected hex signature from client
     * @return true if signatures match
     */
    public static boolean verify(String body, String timestamp, String nonce, String secret, String signatureHex) {
        if (signatureHex == null || signatureHex.isBlank()) {
            return false;
        }
        String expected = sign(body, timestamp, nonce, secret);
        byte[] a = expected.getBytes(StandardCharsets.UTF_8);
        byte[] b = signatureHex.trim().toLowerCase().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte bt : bytes) {
            sb.append(String.format("%02x", bt));
        }
        return sb.toString();
    }
}
