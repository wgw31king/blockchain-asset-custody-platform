package io.github.wahhh.bacp.service;

import io.github.wahhh.bacp.dto.request.DepositNotifyRequest;

/**
 * Normalizes deposit ingestion from REST and messaging adapters.
 */
public interface DepositFacade {

    /**
     * Applies idempotent deposit credit after indexer observation.
     *
     * @param idempotencyKey optional Idempotency-Key header value
     * @param body           parsed payload
     */
    void handleDeposit(String idempotencyKey, DepositNotifyRequest body);
}
