package io.github.wahhh.bacp.custody.spi;

import io.github.wahhh.bacp.dto.request.DepositNotifyRequest;

/**
 * SPI hook for blockchain deposit events consumed by downstream modules (e.g. lottery indexer).
 */
public interface BlockchainEventConsumer {

    /**
     * Handles an inbound deposit notification.
     *
     * @param req parsed deposit payload
     */
    void onDeposit(DepositNotifyRequest req);
}
