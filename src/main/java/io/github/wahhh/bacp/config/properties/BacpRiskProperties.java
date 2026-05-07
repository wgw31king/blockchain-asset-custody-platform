package io.github.wahhh.bacp.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Risk thresholds ({@code bacp.risk.*}).
 */
@Data
@ConfigurationProperties(prefix = "bacp.risk")
public class BacpRiskProperties {

    private java.math.BigDecimal largeAmountThreshold = java.math.BigDecimal.valueOf(10000);

    private int frequencyWindowSeconds = 60;

    private int frequencyMaxCount = 20;
}
