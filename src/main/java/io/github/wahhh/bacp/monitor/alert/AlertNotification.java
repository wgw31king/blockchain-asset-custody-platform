package io.github.wahhh.bacp.monitor.alert;

/**
 * Alert payload dispatched to configured channels.
 *
 * @param level       severity
 * @param subject     short title
 * @param body        plain text or markdown body (used for WeChat; mail HTML defaults from this if htmlBody null)
 * @param htmlBody    optional HTML fragment for email; when null, mail channel escapes {@code body} into a minimal HTML template
 * @param wechatFormat when null, {@code bacp.alert.wechat-work.message-format} is used
 * @param dedupeKey     optional throttle bucket key; when null, derived from level and subject
 */
public record AlertNotification(
        AlertLevel level,
        String subject,
        String body,
        String htmlBody,
        WechatWorkMessageFormat wechatFormat,
        String dedupeKey
) {

    /**
     * Minimal alert without optional fields.
     */
    public static AlertNotification of(AlertLevel level, String subject, String body) {
        return new AlertNotification(level, subject, body, null, null, null);
    }
}
