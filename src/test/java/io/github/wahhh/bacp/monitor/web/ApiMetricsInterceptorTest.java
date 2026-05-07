package io.github.wahhh.bacp.monitor.web;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiMetricsInterceptorTest {

    @Test
    void recordsCountersForStatuses() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ApiMetricsInterceptor interceptor = new ApiMetricsInterceptor(registry);

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/ping");
        MockHttpServletResponse res = new MockHttpServletResponse();
        res.setStatus(200);
        interceptor.afterCompletion(req, res, new Object(), null);

        assertEquals(1, registry.get("bacp_http_requests_total")
                .counter().count(), 0.001);

        MockHttpServletResponse res5 = new MockHttpServletResponse();
        res5.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        interceptor.afterCompletion(req, res5, new Object(), null);

        assertEquals(1.0, sumErrors(registry), 0.001);

        MockHttpServletResponse res429 = new MockHttpServletResponse();
        res429.setStatus(429);
        interceptor.afterCompletion(req, res429, new Object(), null);
        assertEquals(2.0, sumErrors(registry), 0.001);
    }

    private static double sumErrors(SimpleMeterRegistry registry) {
        return registry.find("bacp_api_errors_total").counters().stream().mapToDouble(Counter::count).sum();
    }

    @Test
    void sanitizeUriStripsQuery() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/foo?x=1");
        assertEquals("/api/foo", ApiMetricsInterceptor.sanitizeUri(req));
    }
}
