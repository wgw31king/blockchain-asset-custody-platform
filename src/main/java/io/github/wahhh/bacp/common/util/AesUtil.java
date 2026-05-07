package io.github.wahhh.bacp.common.util;

import io.github.wahhh.bacp.common.exception.BizException;
import io.github.wahhh.bacp.common.result.ResultCode;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM helpers for encrypting sensitive strings at rest (e.g. private keys).
 */
public final class AesUtil {

    private static final String AES = "AES";

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private static final int GCM_IV_LENGTH = 12;

    private static final int GCM_TAG_BITS = 128;

    private static final SecureRandom RANDOM = new SecureRandom();

    private AesUtil() {
    }

    /**
     * Encrypts plaintext using AES-256-GCM; output is Base64(iv || ciphertext||tag).
     *
     * @param plaintext arbitrary UTF-8 string
     * @param key       passphrase normalized to 32-byte key via SHA-256
     * @return Base64 ciphertext bundle
     */
    public static String encrypt(String plaintext, String key) {
        try {
            SecretKey secretKey = toKey(key);
            byte[] iv = new byte[GCM_IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception ex) {
            throw new BizException(ResultCode.INTERNAL_ERROR, "AES encrypt failed");
        }
    }

    /**
     * Decrypts payload produced by {@link #encrypt(String, String)}.
     *
     * @param base64Cipher Base64(iv || ciphertext||tag)
     * @param key          same key material as encrypt
     * @return original plaintext
     */
    public static String decrypt(String base64Cipher, String key) {
        try {
            byte[] all = Base64.getDecoder().decode(base64Cipher);
            if (all.length <= GCM_IV_LENGTH) {
                throw new BizException(ResultCode.INTERNAL_ERROR, "invalid cipher bundle");
            }
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(all, 0, iv, 0, GCM_IV_LENGTH);
            byte[] ct = new byte[all.length - GCM_IV_LENGTH];
            System.arraycopy(all, GCM_IV_LENGTH, ct, 0, ct.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, toKey(key), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] pt = cipher.doFinal(ct);
            return new String(pt, StandardCharsets.UTF_8);
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(ResultCode.INTERNAL_ERROR, "AES decrypt failed");
        }
    }

    private static SecretKey toKey(String keyMaterial) throws Exception {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] raw = sha256.digest(keyMaterial.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(raw, AES);
    }
}
