package io.github.wahhh.bacp.monitor.notifier;

import io.github.wahhh.bacp.config.properties.BacpAlertProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Log-only WeChat Work webhook stub driven by {@link BacpAlertProperties#getWechatWork()}.
 */
@Component
public class WeChatWorkNotifier implements AlertNotifier {

    private static final Logger log = LoggerFactory.getLogger(WeChatWorkNotifier.class);

    private final BacpAlertProperties alertProperties;

    /**
     * @param alertProperties alert configuration
     */
    public WeChatWorkNotifier(BacpAlertProperties alertProperties) {
        this.alertProperties = alertProperties;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendAlert(String subject, String body) {
        BacpAlertProperties.WechatWork ww = alertProperties.getWechatWork();
        if (!ww.isEnabled()) {
            log.debug("[wechat-work-notifier] skipped (disabled): {}", subject);
            return;
        }
        log.info("[wechat-work-notifier] stub webhook configured={} subject={} body={}",
                ww.getWebhook() != null && !ww.getWebhook().isBlank(), subject, body);
    }
}
