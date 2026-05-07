package io.github.wahhh.bacp.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Alert notification stubs ({@code bacp.alert.*}).
 */
@Data
@ConfigurationProperties(prefix = "bacp.alert")
public class BacpAlertProperties {

    private Mail mail = new Mail();

    private Dingtalk dingtalk = new Dingtalk();

    private WechatWork wechatWork = new WechatWork();

    @Data
    public static class Mail {

        private boolean enabled;

        private String to = "";
    }

    @Data
    public static class Dingtalk {

        private boolean enabled;

        private String webhook = "";
    }

    @Data
    public static class WechatWork {

        private boolean enabled;

        private String webhook = "";
    }
}
