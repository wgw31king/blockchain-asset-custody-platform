package io.github.wahhh.bacp.common.constant;

/**
 * Redis cache key prefixes used across modules.
 */
public final class CacheKeys {

    private CacheKeys() {
    }

    /** User permission cache: bacp:user:perms:{userId}. */
    public static final String USER_PERMS = "bacp:user:perms:";

    /** User profile cache: bacp:user:info:{userId}. */
    public static final String USER_INFO = "bacp:user:info:";

    /** Idempotency guard: bacp:idem:{key}. */
    public static final String IDEMPOTENCY = "bacp:idem:";

    /** Anti-replay nonce store: bacp:nonce:{nonce}. */
    public static final String NONCE = "bacp:nonce:";

    /** Rate limit bucket state: bacp:rl:{key}. */
    public static final String RATE_LIMIT = "bacp:rl:";

    /** Cached latest chain block height: bacp:chain:block:{chain}. */
    public static final String CHAIN_BLOCK = "bacp:chain:block:";
}
