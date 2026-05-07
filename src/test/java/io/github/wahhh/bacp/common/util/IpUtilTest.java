package io.github.wahhh.bacp.common.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IpUtilTest {

    @Test
    void prefersForwardedHeader() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Forwarded-For", "203.0.113.1, 10.0.0.1");
        assertEquals("203.0.113.1", IpUtil.getClientIp(req));
    }

    @Test
    void fallsBackToRemoteAddr() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("192.168.0.5");
        assertEquals("192.168.0.5", IpUtil.getClientIp(req));
    }

    @Test
    void nullRequestEmpty() {
        assertTrue(IpUtil.getClientIp(null).isEmpty());
    }
}
