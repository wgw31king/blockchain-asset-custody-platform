package io.github.wahhh.bacp.monitor.notifier;

import io.github.wahhh.bacp.config.properties.BacpAlertProperties;
import io.github.wahhh.bacp.monitor.alert.AlertChannelIds;
import io.github.wahhh.bacp.monitor.alert.AlertNotification;
import io.github.wahhh.bacp.monitor.alert.AlertRetrySupport;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * SMTP alert channel driven by {@link BacpAlertProperties#getMail()}.
 */
@Component
public class MailNotifier implements AlertChannel {

    private static final Logger log = LoggerFactory.getLogger(MailNotifier.class);

    private final BacpAlertProperties alertProperties;

    private final ObjectProvider<JavaMailSender> mailSender;

    /**
     * @param alertProperties bacp alert settings
     * @param mailSender      present only when SMTP is configured
     */
    public MailNotifier(BacpAlertProperties alertProperties, ObjectProvider<JavaMailSender> mailSender) {
        this.alertProperties = alertProperties;
        this.mailSender = mailSender;
    }

    @Override
    public String getChannelId() {
        return AlertChannelIds.MAIL;
    }

    @Override
    public void send(AlertNotification notification) {
        BacpAlertProperties.Mail mail = alertProperties.getMail();
        if (!mail.isEnabled()) {
            log.debug("[mail-notifier] skipped (disabled): {}", notification.subject());
            return;
        }
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.debug("[mail-notifier] skipped (no JavaMailSender bean): {}", notification.subject());
            return;
        }
        if (mail.getTo() == null || mail.getTo().isBlank()) {
            log.warn("[mail-notifier] skipped (bacp.alert.mail.to empty): {}", notification.subject());
            return;
        }
        String[] recipients = parseAddresses(mail.getTo());
        if (recipients.length == 0) {
            log.warn("[mail-notifier] skipped (no valid recipients): {}", notification.subject());
            return;
        }
        AlertRetrySupport.runWithRetry("mail-smtp", () -> {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(resolveFrom(mail));
            helper.setTo(recipients);
            helper.setSubject(notification.subject());
            helper.setText(resolveHtml(notification), true);
            sender.send(message);
        });
    }

    private static String resolveFrom(BacpAlertProperties.Mail mail) {
        if (mail.getFrom() != null && !mail.getFrom().isBlank()) {
            return mail.getFrom();
        }
        if (mail.getUsername() != null && mail.getUsername().contains("@")) {
            return mail.getUsername();
        }
        return "noreply@" + mail.getHost();
    }

    private static String[] parseAddresses(String to) {
        return Arrays.stream(to.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList())
                .toArray(String[]::new);
    }

    private static String resolveHtml(AlertNotification notification) {
        if (notification.htmlBody() != null && !notification.htmlBody().isBlank()) {
            return notification.htmlBody();
        }
        return "<html><body><h2>" + escapeHtml(notification.subject()) + "</h2><pre>"
                + escapeHtml(notification.body()) + "</pre></body></html>";
    }

    private static String escapeHtml(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
