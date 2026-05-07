package io.github.wahhh.bacp.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Supported blockchain networks for custody operations.
 */
@Getter
@RequiredArgsConstructor
public enum ChainType {

    /** Ethereum mainnet / layer-1 compatible chains. */
    ETH("ethereum"),

    /** Binance Smart Chain. */
    BSC("bsc"),

    /** Polygon PoS. */
    POLYGON("polygon");

    private final String code;

    /**
     * Resolves {@link ChainType} from lowercase chain code.
     *
     * @param code external chain identifier
     * @return matching enum constant
     */
    public static ChainType fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("chain code must not be blank");
        }
        String normalized = code.trim().toLowerCase();
        for (ChainType t : values()) {
            if (t.code.equalsIgnoreCase(normalized)) {
                return t;
            }
        }
        throw new IllegalArgumentException("unknown chain code: " + code);
    }
}
