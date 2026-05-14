package io.github.wahhh.bacp.config.properties;

import io.github.wahhh.bacp.monitor.alert.WechatWorkMessageFormat;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Alert notification channels ({@code bacp.alert.*}).
 */
@Data
@ConfigurationProperties(prefix = "bacp.alert")
public class BacpAlertProperties {

    private Mail mail = new Mail();

    private Dingtalk dingtalk = new Dingtalk();

    private WechatWork wechatWork = new WechatWork();

    private Routing routing = new Routing();

    private Throttle throttle = new Throttle();

    @Data
    public static class Mail {

        /** When false, no SMTP connection is attempted and mail bean may be absent. */
        private boolean enabled;

        private String host = "";

        private int port = 587;

        private String username = "";

        private String password = "";

        private String from = "";

        private String to = "";

        /** When true, use implicit SSL (typically port 465). */
        private boolean ssl = false;

        /** When true, enable STARTTLS (typical for port 587). */
        private boolean startTls = true;
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

        /** Used when {@link io.github.wahhh.bacp.monitor.alert.AlertNotification#wechatFormat()} is null. */
        private WechatWorkMessageFormat messageFormat = WechatWorkMessageFormat.MARKDOWN;
    }

    /**
     * Routes each severity to channel ids ({@code mail}, {@code wechat-work}, {@code dingtalk}). Empty by default.
     */
    @Data
    public static class Routing {

        private List<String> info = new ArrayList<>();

        private List<String> warn = new ArrayList<>();

        private List<String> error = new ArrayList<>();

        private List<String> fatal = new ArrayList<>();
    }

    /**
     * In-memory throttle per dedupe/window key (single-node; cluster-wide limits need Redis).
     */
    @Data
    public static class Throttle {

        private boolean enabled = true;

        /** Sliding window length. */
        private int windowSeconds = 60;

        /** Max alerts allowed per window per throttle key. */
        private int maxPerWindow = 30;
    }
}
