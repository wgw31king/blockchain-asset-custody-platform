package io.github.wahhh.bacp.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Tunables for distributed matching, TTL, and fee defaults.
 */
@Data
@ConfigurationProperties(prefix = "bacp.trade")
public class BacpTradeProperties {

    /**
     * Max time to wait for the per-symbol Redisson lock (milliseconds).
     */
    private long lockWaitMs = 200L;

    /**
     * Lock lease after acquisition; must exceed worst-case single match transaction duration.
     */
    private long lockLeaseMs = 5000L;

    /**
     * Order lifetime from creation; expired resting orders are auto-cancelled by the scheduler.
     */
    private long orderTtlSeconds = 86400L;

    /**
     * Scheduler cadence for scanning expired orders (fixed delay ms).
     */
    private long timeoutPollMs = 30_000L;

    /**
     * Safety cap on maker iterations per placement to avoid runaway loops.
     */
    private int maxMatchIterations = 200;

    /**
     * Flat fee rate applied to received asset on each fill (0 = disabled).
     */
    private BigDecimal feeRate = BigDecimal.ZERO;
}
