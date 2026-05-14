package io.github.wahhh.bacp.common.constant;

/**
 * Values written to {@code t_capital_flow.direction} for trade-related balance mutations.
 */
public final class TradeLedgerDirection {

    private TradeLedgerDirection() {
    }

    /** Moves amount from available to frozen (order reservation). */
    public static final String FREEZE = "TRADE_FREEZE";

    /** Moves amount from frozen back to available (cancel / timeout / surplus). */
    public static final String UNFREEZE = "TRADE_UNFREEZE";

    /** Consumes previously frozen funds when a fill executes. */
    public static final String SETTLE_SPEND = "TRADE_SETTLE_SPEND";

    /** Credits available balance from a counterparty fill (post-fee). */
    public static final String SETTLE_RECEIVE = "TRADE_SETTLE_RECEIVE";
}
