package io.github.wahhh.bacp.service;

import io.github.wahhh.bacp.entity.Wallet;

/**
 * Generates and retrieves custodial wallets per chain.
 */
public interface WalletService {

    /**
     * Ensures a wallet exists for the user on the given chain profile.
     *
     * @param userId      user id
     * @param chainProfile ethereum | bsc | polygon profile key
     * @return persisted wallet row
     */
    Wallet ensureWallet(Long userId, String chainProfile);
}
