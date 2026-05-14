package io.github.wahhh.bacp;

import io.github.wahhh.bacp.config.properties.BacpAlertProperties;
import io.github.wahhh.bacp.config.properties.BacpCryptoProperties;
import io.github.wahhh.bacp.config.properties.BacpCustodyProperties;
import io.github.wahhh.bacp.config.properties.BacpRateLimitProperties;
import io.github.wahhh.bacp.config.properties.BacpRiskProperties;
import io.github.wahhh.bacp.config.properties.BacpSecurityProperties;
import io.github.wahhh.bacp.config.properties.BacpMetricsProperties;
import io.github.wahhh.bacp.config.properties.BacpTradeProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Blockchain Asset Custody Platform (BACP) entry point.
 *
 * @author wahhh
 */
@SpringBootApplication(exclude = MailSenderAutoConfiguration.class)
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties({
        BacpSecurityProperties.class,
        BacpCryptoProperties.class,
        BacpCustodyProperties.class,
        BacpRiskProperties.class,
        BacpAlertProperties.class,
        BacpRateLimitProperties.class,
        BacpTradeProperties.class,
        BacpMetricsProperties.class
})
@MapperScan("io.github.wahhh.bacp.**.mapper")
public class BACPApplication {

    public static void main(String[] args) {
        SpringApplication.run(BACPApplication.class, args);
    }
}
