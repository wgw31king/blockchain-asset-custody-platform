package io.github.wahhh.bacp.common.constant;

/**
 * Security-related string constants for HTTP headers and Redis keys.
 */
public final class SecurityConstants {

    private SecurityConstants() {
    }

    /** Authorization HTTP header name. */
    public static final String AUTH_HEADER = "Authorization";

    /** Bearer scheme prefix. */
    public static final String BEARER_PREFIX = "Bearer ";

    /** Redis key prefix for login failure counters. */
    public static final String LOGIN_FAIL_KEY = "bacp:login:fail:";

    /** Redis key prefix for revoked JWT identifiers. */
    public static final String JWT_BLACKLIST_KEY = "bacp:jwt:blacklist:";

    /** Wildcard permission granting all authorities. */
    public static final String PERM_ALL = "*";

    /** Spring Security role prefix. */
    public static final String ROLE_PREFIX = "ROLE_";
}
