package io.github.wahhh.bacp.common.enums;

/**
 * Spot order lifecycle for the simplified matching engine.
 */
public enum OrderStatus {

    /** Resting in the order book. */
    OPEN,

    /** Partially matched. */
    PARTIALLY_FILLED,

    /** Fully matched. */
    FILLED,

    /** Canceled by user or system. */
    CANCELED
}
