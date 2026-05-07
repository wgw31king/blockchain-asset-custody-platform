package io.github.wahhh.bacp.common.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves client IP behind proxies.
 */
public final class IpUtil {

    private static final String UNKNOWN = "unknown";

    private IpUtil() {
    }

    /**
     * Extracts best-effort client IP from servlet request.
     *
     * @param request HTTP request
     * @return IPv4/IPv6 string without zone id noise where possible
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String ip = header(request, "X-Forwarded-For");
        if (ip == null || ip.isBlank() || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = header(request, "X-Real-IP");
        }
        if (ip == null || ip.isBlank() || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        if (ip != null && ip.startsWith("[") && ip.contains("]:")) {
            ip = ip.substring(1, ip.indexOf(']'));
        } else if (ip != null && ip.contains(":") && ip.lastIndexOf(':') > ip.indexOf(':')) {
            int last = ip.lastIndexOf(':');
            ip = ip.substring(0, last);
        }
        return ip == null ? "" : ip;
    }

    private static String header(HttpServletRequest request, String name) {
        String v = request.getHeader(name);
        return v == null ? "" : v.trim();
    }
}
