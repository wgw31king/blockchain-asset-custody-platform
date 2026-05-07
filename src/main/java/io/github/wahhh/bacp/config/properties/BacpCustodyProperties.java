package io.github.wahhh.bacp.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Custody defaults ({@code bacp.custody.*}).
 */
@Data
@ConfigurationProperties(prefix = "bacp.custody")
public class BacpCustodyProperties {

    private int defaultConfirmations = 12;

    private String withdrawMinAmount = "0.0001";

    private String withdrawMaxAmount = "1000";

    private Map<String, ChainRpc> chains = new HashMap<>();

    @Data
    public static class ChainRpc {

        private String rpcUrl = "";

        private long chainId;
    }
}
