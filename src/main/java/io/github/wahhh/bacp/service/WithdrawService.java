package io.github.wahhh.bacp.service;

import io.github.wahhh.bacp.dto.request.WithdrawRequest;

/**
 * Withdraw orchestration (risk hooks + async broadcast).
 */
public interface WithdrawService {

    /**
     * Validates balances and enqueues withdrawal processing.
     *
     * @param userId  authenticated user
     * @param request payload
     * @return created tx id
     */
    Long submit(Long userId, WithdrawRequest request);
}
