package io.github.wahhh.bacp.monitor.notifier;

/**
 * Pluggable alert sink (mail, chat webhooks, etc.).
 */
public interface AlertNotifier {

    /**
     * Sends or records an operational alert.
     *
     * @param subject short title
     * @param body    detail text
     */
    void sendAlert(String subject, String body);
}
