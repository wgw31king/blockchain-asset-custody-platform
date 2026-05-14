package io.github.wahhh.bacp.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Observability tunables: SQL thresholds, DAU Redis keys, disk gauge path.
 */
@Data
@ConfigurationProperties(prefix = "bacp.metrics")
public class BacpMetricsProperties {

    /**
     * Chain head polling interval for {@code bacp_chain_head_block} (milliseconds).
     */
    private long chainPollMs = 30_000L;

    /**
     * Statements slower than this increment {@code bacp_sql_slow_total} and {@code bacp_sql_slow_seconds}.
     */
    private long sqlSlowThresholdMs = 500L;

    /**
     * TTL for Redis HyperLogLog keys used by approximate DAU ({@code bacp_user_active_daily_approx}).
     */
    private long dauKeyTtlHours = 48L;

    /**
     * How often to refresh the DAU gauge from Redis {@code PFCOUNT} (milliseconds).
     */
    private long dauRefreshMs = 60_000L;

    /**
     * Path passed to {@link java.nio.file.FileStore} for {@code bacp_disk_free_bytes} (container root is typical).
     */
    private String diskFreePath = "/";

    /**
     * Polling interval for {@link io.github.wahhh.bacp.monitor.metrics.DiskSpaceMetrics} (milliseconds).
     */
    private long diskPollMs = 60_000L;

}
