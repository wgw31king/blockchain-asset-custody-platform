package io.github.wahhh.bacp.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Defaults for Redis token-bucket rate limiting ({@code bacp.ratelimit.*}).
 */
@Data
@ConfigurationProperties(prefix = "bacp.ratelimit")
public class BacpRateLimitProperties {

    /** Maximum burst tokens per key when annotation does not override. */
    private int defaultCapacity = 100;

    /** Token refill rate per wall-clock second. */
    private double defaultRefillPerSec = 50d;
}
