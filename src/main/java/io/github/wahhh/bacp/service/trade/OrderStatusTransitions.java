package io.github.wahhh.bacp.service.trade;

import io.github.wahhh.bacp.common.enums.OrderStatus;
import io.github.wahhh.bacp.common.exception.BizException;
import io.github.wahhh.bacp.common.result.ResultCode;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Validates legal {@link OrderStatus} moves for the matching engine.
 *
 * <p>PROCESSING→PENDING is allowed so a LIMIT order with zero fills can return to the book after a
 * match cycle (the plan's strict PROCESSING targets did not include a "resting" terminal for
 * unfilled quantity; PENDING represents an open resting order).</p>
 */
public final class OrderStatusTransitions {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = new EnumMap<>(OrderStatus.class);

    static {
        // PENDING may become FILLED / PARTIALLY_FILLED when this order is the passive maker matched
        // by another user's taker (no PROCESSING hop on the maker row).
        ALLOWED.put(OrderStatus.PENDING,
                EnumSet.of(OrderStatus.PROCESSING, OrderStatus.PARTIALLY_FILLED, OrderStatus.FILLED,
                        OrderStatus.CANCELLED, OrderStatus.REJECTED));
        ALLOWED.put(OrderStatus.PROCESSING,
                EnumSet.of(OrderStatus.PARTIALLY_FILLED, OrderStatus.FILLED, OrderStatus.CANCELLED,
                        OrderStatus.REJECTED, OrderStatus.PENDING));
        ALLOWED.put(OrderStatus.PARTIALLY_FILLED,
                EnumSet.of(OrderStatus.PROCESSING, OrderStatus.PARTIALLY_FILLED, OrderStatus.FILLED,
                        OrderStatus.CANCELLED));
        ALLOWED.put(OrderStatus.FILLED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED.put(OrderStatus.REJECTED, EnumSet.noneOf(OrderStatus.class));
    }

    private OrderStatusTransitions() {
    }

    /**
     * Ensures {@code to} is reachable from {@code from}; throws when the transition is illegal.
     *
     * @param from current persisted status
     * @param to   desired next status
     */
    public static void assertCanTransition(OrderStatus from, OrderStatus to) {
        if (from == to) {
            return;
        }
        Set<OrderStatus> next = ALLOWED.get(from);
        if (next == null || !next.contains(to)) {
            throw new BizException(ResultCode.BAD_REQUEST,
                    "illegal order status transition: " + from + " -> " + to);
        }
    }

    /**
     * Parses persisted status string or throws a domain validation error.
     *
     * @param status raw DB / API value
     * @return enum constant
     */
    public static OrderStatus parse(String status) {
        if (status == null || status.isBlank()) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "order status missing");
        }
        try {
            return OrderStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "unknown order status: " + status);
        }
    }
}
