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

    /** Per-symbol matcher Redisson lock: bacp:trade:lock:{SYMBOL}. */
    public static final String TRADE_SYMBOL_LOCK = "bacp:trade:lock:";

    /** Cached tradable symbol definition JSON: bacp:trade:symdef:{SYMBOL}. */
    public static final String TRADE_SYMBOL_DEF = "bacp:trade:symdef:";

    /** HyperLogLog approximate DAU keys: bacp:metrics:dau:{yyyyMMdd}. */
    public static final String METRICS_DAU_PREFIX = "bacp:metrics:dau:";

    /** First observation time for deposit confirmation latency: bacp:metrics:deposit:first_seen:{txHash}. */
    public static final String METRICS_DEPOSIT_FIRST_SEEN = "bacp:metrics:deposit:first_seen:";
}
