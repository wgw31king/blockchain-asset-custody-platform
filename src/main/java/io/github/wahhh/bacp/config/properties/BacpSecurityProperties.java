package io.github.wahhh.bacp.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Security-related BACP settings ({@code bacp.security.*}).
 */
@Data
@ConfigurationProperties(prefix = "bacp.security")
public class BacpSecurityProperties {

    private Jwt jwt = new Jwt();

    private Login login = new Login();

    /** Comma-separated IPs allowed for {@code /api/v1/admin/**}. */
    private String adminIpWhitelist = "127.0.0.1,0:0:0:0:0:0:0:1";

    private RequestSigning requestSigning = new RequestSigning();

    @Data
    public static class Jwt {

        private String secret;

        private long accessTokenTtlSeconds = 3600L;

        private long refreshTokenTtlSeconds = 604800L;

        private String issuer = "bacp";

        private String audience = "bacp-api";
    }

    @Data
    public static class Login {

        private int maxFailCount = 5;

        private int lockMinutes = 30;
    }

    @Data
    public static class RequestSigning {

        private boolean enabled;

        private long ttlSeconds = 300L;

        private String secret = "";
    }
}
