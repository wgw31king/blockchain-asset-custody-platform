package io.github.wahhh.bacp.service.trade;

import io.github.wahhh.bacp.common.enums.OrderStatus;
import io.github.wahhh.bacp.common.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Persists order lifecycle rows into {@code t_sys_operation_log} (same table as {@code OperationLogAspect})
 * so every status transition is auditable even when no HTTP context exists (scheduler / matcher).
 */
@Component
@RequiredArgsConstructor
public class OrderAuditWriter {

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    /**
     * Records an order status transition with structured JSON params.
     *
     * @param userId  acting user (owner or null for system)
     * @param orderId order id
     * @param from    previous status
     * @param to      new status
     * @param extra   optional diagnostic fields (symbol, qty, etc.)
     */
    public void writeOrderStatus(Long userId, Long orderId, OrderStatus from, OrderStatus to, Map<String, Object> extra) {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", orderId);
        payload.put("from", from.name());
        payload.put("to", to.name());
        if (extra != null && !extra.isEmpty()) {
            payload.putAll(extra);
        }
        String username = userId != null ? "user:" + userId : "system";
        jdbc.update(
                """
                        INSERT INTO t_sys_operation_log
                        (user_id, username, module, action, params, ip, success, error_msg, duration_ms, created_at)
                        VALUES (?,?,?,?,?,?,?,?,?,?)
                        """,
                userId,
                username,
                "trade",
                "ORDER_STATUS",
                JsonUtil.toJson(payload),
                "",
                1,
                null,
                0L,
                LocalDateTime.now());
    }
}
