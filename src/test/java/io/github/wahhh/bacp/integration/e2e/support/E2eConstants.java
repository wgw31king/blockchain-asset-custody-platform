package io.github.wahhh.bacp.integration.e2e.support;

/**
 * Shared credentials and prefixes for full-stack E2E tests (see bacp_tc_init.sql).
 */
public final class E2eConstants {

    public static final String ADMIN_USERNAME = "e2e-admin";

    /** Matches BCrypt hash seeded in {@code bacp_tc_init.sql}. */
    public static final String ADMIN_PASSWORD = "E2EAdmin@123";

    public static final String EPHEMERAL_USER_PREFIX = "e2e-u-";

    private E2eConstants() {
    }
}
