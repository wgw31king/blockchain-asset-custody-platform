package io.github.wahhh.bacp.monitor.notifier;

import io.github.wahhh.bacp.config.properties.BacpAlertProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Log-only DingTalk webhook stub driven by {@link BacpAlertProperties#getDingtalk()}.
 */
@Component
public class DingTalkNotifier implements AlertNotifier {

    private static final Logger log = LoggerFactory.getLogger(DingTalkNotifier.class);

    private final BacpAlertProperties alertProperties;

    /**
     * @param alertProperties alert configuration
     */
    public DingTalkNotifier(BacpAlertProperties alertProperties) {
        this.alertProperties = alertProperties;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendAlert(String subject, String body) {
        BacpAlertProperties.Dingtalk ding = alertProperties.getDingtalk();
        if (!ding.isEnabled()) {
            log.debug("[dingtalk-notifier] skipped (disabled): {}", subject);
            return;
        }
        log.info("[dingtalk-notifier] stub webhook configured={} subject={} body={}",
                ding.getWebhook() != null && !ding.getWebhook().isBlank(), subject, body);
    }
}
