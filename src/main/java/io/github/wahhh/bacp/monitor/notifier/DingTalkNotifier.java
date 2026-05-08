package io.github.wahhh.bacp.monitor.notifier;

import io.github.wahhh.bacp.config.properties.BacpAlertProperties;
import io.github.wahhh.bacp.monitor.alert.AlertChannelIds;
import io.github.wahhh.bacp.monitor.alert.AlertNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Log-only DingTalk stub driven by {@link BacpAlertProperties#getDingtalk()}.
 */
@Component
public class DingTalkNotifier implements AlertChannel {

    private static final Logger log = LoggerFactory.getLogger(DingTalkNotifier.class);

    private final BacpAlertProperties alertProperties;

    /**
     * @param alertProperties alert configuration
     */
    public DingTalkNotifier(BacpAlertProperties alertProperties) {
        this.alertProperties = alertProperties;
    }

    @Override
    public String getChannelId() {
        return AlertChannelIds.DINGTALK;
    }

    @Override
    public void send(AlertNotification notification) {
        BacpAlertProperties.Dingtalk ding = alertProperties.getDingtalk();
        if (!ding.isEnabled()) {
            log.debug("[dingtalk-notifier] skipped (disabled): {}", notification.subject());
            return;
        }
        log.info("[dingtalk-notifier] stub webhook configured={} level={} subject={} body={}",
                ding.getWebhook() != null && !ding.getWebhook().isBlank(),
                notification.level(),
                notification.subject(),
                notification.body());
    }
}
