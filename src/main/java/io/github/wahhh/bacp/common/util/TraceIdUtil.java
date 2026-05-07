package io.github.wahhh.bacp.common.util;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * Distributed tracing correlation id helpers using SLF4J {@link MDC}.
 */
public final class TraceIdUtil {

    public static final String MDC_KEY = "traceId";

    private TraceIdUtil() {
    }

    /**
     * Puts trace id into MDC.
     *
     * @param traceId correlation id
     */
    public static void put(String traceId) {
        if (traceId != null && !traceId.isBlank()) {
            MDC.put(MDC_KEY, traceId);
        }
    }

    /**
     * Clears trace id from MDC.
     */
    public static void clear() {
        MDC.remove(MDC_KEY);
    }

    /**
     * Returns existing trace id or generates a new UUID string.
     *
     * @param headerValue optional inbound header
     * @return trace id
     */
    public static String getOrGenerate(String headerValue) {
        if (headerValue != null && !headerValue.isBlank()) {
            return headerValue.trim();
        }
        String existing = MDC.get(MDC_KEY);
        if (existing != null && !existing.isBlank()) {
            return existing;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
