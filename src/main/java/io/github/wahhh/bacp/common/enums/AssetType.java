package io.github.wahhh.bacp.common.enums;

/**
 * Asset classification on-chain.
 */
public enum AssetType {

    /** Native coin (ETH, BNB, MATIC). */
    NATIVE,

    /** ERC-20 fungible token. */
    ERC20,

    /** ERC-721 non-fungible token. */
    ERC721,

    /** ERC-1155 semi-fungible token. */
    ERC1155
}
