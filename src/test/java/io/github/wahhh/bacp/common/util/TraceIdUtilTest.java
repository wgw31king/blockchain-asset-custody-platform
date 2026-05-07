package io.github.wahhh.bacp.common.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
class TraceIdUtilTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void putAndClear() {
        TraceIdUtil.put("abc");
        assertEquals("abc", MDC.get(TraceIdUtil.MDC_KEY));
        TraceIdUtil.clear();
        assertNull(MDC.get(TraceIdUtil.MDC_KEY));
    }

    @Test
    void getOrGenerateUsesHeader() {
        assertEquals("hdr", TraceIdUtil.getOrGenerate(" hdr "));
    }
}
