package io.github.wahhh.bacp.monitor.notifier;

import io.github.wahhh.bacp.monitor.alert.AlertNotification;

/**
 * Pluggable alert sink (mail, chat webhooks, etc.).
 */
public interface AlertChannel {

    /**
     * Stable id matching {@code bacp.alert.routing} entries (e.g. mail, wechat-work).
     *
     * @return channel id
     */
    String getChannelId();

    /**
     * Sends or records an operational alert if this channel is enabled and configured.
     *
     * @param notification alert payload
     */
    void send(AlertNotification notification);
}
