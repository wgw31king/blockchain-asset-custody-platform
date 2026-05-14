package io.github.wahhh.bacp.common.enums;

/**
 * Spot order lifecycle for the production matching engine.
 *
 * <p>PENDING: accepted and resting or awaiting first match attempt. PROCESSING: matcher holds the
 * symbol lock and is mutating this order. Terminal states: FILLED, PARTIALLY_FILLED (may still
 * rest for limit), CANCELLED, REJECTED.</p>
 */
public enum OrderStatus {

    /** Accepted; not yet fully processed or resting on book with no in-flight match. */
    PENDING,

    /** Matcher is actively applying fills for this order (short-lived under per-symbol lock). */
    PROCESSING,

    /** Fully executed. */
    FILLED,

    /** Executed in part; for LIMIT orders may still rest for further matches. */
    PARTIALLY_FILLED,

    /** User or system cancelled remaining quantity. */
    CANCELLED,

    /** Validation / balance / liquidity failure; no resting quantity. */
    REJECTED
}
