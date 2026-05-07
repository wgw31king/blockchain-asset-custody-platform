package io.github.wahhh.bacp.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AesUtilTest {

    @Test
    void roundTrip() {
        String key = "unit-test-passphrase-32bytes-min!!";
        String plain = "custody-secret-value";
        String enc = AesUtil.encrypt(plain, key);
        assertEquals(plain, AesUtil.decrypt(enc, key));
    }
}
