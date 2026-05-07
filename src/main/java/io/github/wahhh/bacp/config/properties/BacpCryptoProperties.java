package io.github.wahhh.bacp.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Secrets for encrypting wallet material at rest ({@code bacp.crypto.*}).
 */
@Data
@ConfigurationProperties(prefix = "bacp.crypto")
public class BacpCryptoProperties {

    private String masterKey = "";
}
