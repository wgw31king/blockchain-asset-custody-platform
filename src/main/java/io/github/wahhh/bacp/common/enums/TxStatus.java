package io.github.wahhh.bacp.common.enums;

/**
 * Lifecycle of on-chain or internal blockchain transactions.
 */
public enum TxStatus {

    /** Pending confirmation or queue processing. */
    PENDING,

    /** Transaction broadcast to network. */
    BROADCASTED,

    /** Sufficient confirmations received. */
    CONFIRMED,

    /** Failed broadcast or reverted. */
    FAILED,

    /** User or operator canceled before broadcast. */
    CANCELED
}
