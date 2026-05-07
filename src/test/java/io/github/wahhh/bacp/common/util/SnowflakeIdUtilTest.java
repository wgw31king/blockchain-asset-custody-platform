package io.github.wahhh.bacp.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SnowflakeIdUtilTest {

    @Test
    void generatesIncreasingIds() {
        long a = SnowflakeIdUtil.nextId();
        long b = SnowflakeIdUtil.nextId();
        assertNotEquals(a, b);
    }
}
