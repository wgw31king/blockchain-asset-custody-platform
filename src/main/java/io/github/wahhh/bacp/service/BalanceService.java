package io.github.wahhh.bacp.service;

import io.github.wahhh.bacp.entity.Balance;

import java.math.BigDecimal;
import java.util.List;

/**
 * Balance mutations with optimistic locking.
 */
public interface BalanceService {

    /**
     * Lists balances for user.
     *
     * @param userId user id
     * @return balances
     */
    List<Balance> list(Long userId);

    /**
     * Credits available balance (creates row if absent).
     *
     * @param userId     user id
     * @param currencyId currency id
     * @param amount     delta
     */
    void credit(Long userId, Long currencyId, BigDecimal amount);

    /**
     * Debits available balance if funds permit.
     *
     * @param userId     user id
     * @param currencyId currency id
     * @param amount     delta
     */
    void debit(Long userId, Long currencyId, BigDecimal amount);
}
