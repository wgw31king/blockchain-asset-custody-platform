package io.github.wahhh.bacp.service;

import io.github.wahhh.bacp.entity.Symbol;

/**
 * Resolves enabled tradable symbols for the matching engine (DB + Redis cache).
 */
public interface SymbolService {

    /**
     * Returns an enabled symbol row or throws when missing/disabled.
     *
     * @param symbol logical pair (e.g. ETH-USDT), case-insensitive
     * @return persisted definition
     */
    Symbol requireEnabled(String symbol);
}
