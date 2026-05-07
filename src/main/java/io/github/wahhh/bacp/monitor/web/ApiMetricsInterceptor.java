package io.github.wahhh.bacp.monitor.web;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Records HTTP traffic counters for {@code /api/**} requests.
 * <p>
 * Always increments {@code bacp_http_requests_total} with tags {@code method}, {@code uri} (path only, query stripped),
 * and {@code status}.
 * <p>
 * Increments {@code bacp_api_errors_total} with tags {@code uri} and {@code status} only when the HTTP status is
 * {@code >= 500} or {@code 429}. This deliberately ignores 4xx responses and JSON {@link io.github.wahhh.bacp.common.result.Result}
 * bodies so tags stay simple and low-cardinality friendly.
 */
@Component
public class ApiMetricsInterceptor implements HandlerInterceptor {

    private final MeterRegistry meterRegistry;

    /**
     * @param meterRegistry registry for HTTP counters
     */
    public ApiMetricsInterceptor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Records {@code bacp_http_requests_total} and optionally {@code bacp_api_errors_total}.
     *
     * @param request  current request
     * @param response current response
     * @param handler  selected handler
     * @param ex       handler exception, if any
     */
    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                @NonNull Object handler, Exception ex) {
        String method = request.getMethod();
        String uri = sanitizeUri(request);
        int status = response.getStatus();
        meterRegistry.counter("bacp_http_requests_total",
                "method", method,
                "uri", uri,
                "status", Integer.toString(status)).increment();
        if (status >= 500 || status == 429) {
            meterRegistry.counter("bacp_api_errors_total",
                    "uri", uri,
                    "status", Integer.toString(status)).increment();
        }
    }

    /**
     * Returns the servlet path without a query string for metric tags.
     *
     * @param request incoming request
     * @return URI path component only
     */
    public static String sanitizeUri(HttpServletRequest request) {
        String uri = request.getRequestURI();
        int q = uri.indexOf('?');
        return q >= 0 ? uri.substring(0, q) : uri;
    }
}
