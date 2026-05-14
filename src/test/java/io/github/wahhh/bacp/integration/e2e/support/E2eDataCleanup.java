package io.github.wahhh.bacp.integration.e2e.support;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * Deletes rows created by E2E flows (never removes {@link E2eConstants#ADMIN_USERNAME}).
 */
public final class E2eDataCleanup {

    private E2eDataCleanup() {
    }

    public static void purgeUser(JdbcTemplate jdbc, long userId) {
        jdbc.update("DELETE FROM t_capital_flow WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM t_trade WHERE buyer_id = ? OR seller_id = ?", userId, userId);
        jdbc.update("DELETE FROM t_order WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM t_tx_record WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM t_balance WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM t_wallet WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM t_sys_user_role WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM t_sys_user WHERE id = ?", userId);
    }

    /** Removes all users with username {@code e2e-u-%} (ephemeral test accounts). */
    public static void purgeEphemeralUsers(JdbcTemplate jdbc) {
        List<Long> ids = jdbc.queryForList(
                "SELECT id FROM t_sys_user WHERE username LIKE ?", Long.class, E2eConstants.EPHEMERAL_USER_PREFIX + "%");
        for (Long id : ids) {
            purgeUser(jdbc, id);
        }
    }
}
