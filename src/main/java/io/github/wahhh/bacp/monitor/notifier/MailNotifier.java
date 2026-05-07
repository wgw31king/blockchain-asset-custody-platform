package io.github.wahhh.bacp.monitor.notifier;

import io.github.wahhh.bacp.config.properties.BacpAlertProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Log-only mail stub driven by {@link BacpAlertProperties#getMail()}.
 */
@Component
public class MailNotifier implements AlertNotifier {

    private static final Logger log = LoggerFactory.getLogger(MailNotifier.class);

    private final BacpAlertProperties alertProperties;

    /**
     * @param alertProperties alert configuration
     */
    public MailNotifier(BacpAlertProperties alertProperties) {
        this.alertProperties = alertProperties;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendAlert(String subject, String body) {
        BacpAlertProperties.Mail mail = alertProperties.getMail();
        if (!mail.isEnabled()) {
            log.debug("[mail-notifier] skipped (disabled): {}", subject);
            return;
        }
        log.info("[mail-notifier] stub to={} subject={} body={}",
                mail.getTo(), subject, body);
    }
}
