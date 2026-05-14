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

    /**
     * Moves funds from available to frozen for an order reservation.
     *
     * @param userId     user id
     * @param currencyId currency id
     * @param amount     amount to freeze
     * @param refType    reference domain (e.g. ORDER)
     * @param refId      reference id (e.g. order id)
     */
    void freeze(Long userId, Long currencyId, BigDecimal amount, String refType, Long refId);

    /**
     * Moves funds from frozen back to available (cancel, surplus, timeout).
     *
     * @param userId     user id
     * @param currencyId currency id
     * @param amount     amount to release
     * @param refType    reference domain
     * @param refId      reference id
     */
    void unfreeze(Long userId, Long currencyId, BigDecimal amount, String refType, Long refId);

    /**
     * Deducts matched notion from frozen without touching available (fill settlement spend leg).
     *
     * @param userId     user id
     * @param currencyId currency id
     * @param amount     amount to consume from frozen
     * @param refType    reference domain
     * @param refId      reference id (often trade id)
     */
    void settleSpendFrozen(Long userId, Long currencyId, BigDecimal amount, String refType, Long refId);

    /**
     * Credits available balance for the receive leg of a fill (typically post-fee).
     *
     * @param userId     user id
     * @param currencyId currency id
     * @param amount     credit to available
     * @param refType    reference domain
     * @param refId      reference id (often trade id)
     */
    void settleReceiveAvailable(Long userId, Long currencyId, BigDecimal amount, String refType, Long refId);
}
