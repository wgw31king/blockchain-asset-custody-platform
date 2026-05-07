package io.github.wahhh.bacp.common.enums;

/**
 * Direction of funds movement for ledger entries.
 */
public enum TxDirection {

    /** On-chain deposit credited to user balance. */
    DEPOSIT,

    /** User-initiated withdrawal to external address. */
    WITHDRAW,

    /** Internal transfer between platform accounts. */
    TRANSFER,

    /** Buy side of spot trading. */
    TRADE_BUY,

    /** Sell side of spot trading. */
    TRADE_SELL
}
