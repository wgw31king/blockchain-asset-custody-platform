package io.github.wahhh.bacp.monitor.alert;

import io.github.wahhh.bacp.config.properties.BacpAlertProperties;
import io.github.wahhh.bacp.monitor.notifier.AlertChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Dispatches {@link AlertNotification} to channels listed in {@code bacp.alert.routing} after throttle checks.
 */
@Service
public class AlertNotificationService {

    private static final Logger log = LoggerFactory.getLogger(AlertNotificationService.class);

    private final BacpAlertProperties alertProperties;

    private final AlertThrottle throttle;

    private final Map<String, AlertChannel> channelsById;

    public AlertNotificationService(BacpAlertProperties alertProperties,
                                    AlertThrottle throttle,
                                    List<AlertChannel> channels) {
        this.alertProperties = alertProperties;
        this.throttle = throttle;
        this.channelsById = channels.stream()
                .collect(Collectors.toMap(AlertChannel::getChannelId, Function.identity(), (a, b) -> a,
                        ConcurrentHashMap::new));
    }

    /**
     * Routes the alert to configured channels for its level.
     *
     * @param notification alert payload
     */
    public void notify(AlertNotification notification) {
        String throttleKey = notification.dedupeKey() != null && !notification.dedupeKey().isBlank()
                ? notification.dedupeKey()
                : notification.level().name() + ":" + notification.subject();
        if (!throttle.tryAcquire(throttleKey)) {
            log.warn("[alert] throttled key={} subject={}", throttleKey, notification.subject());
            return;
        }
        List<String> route = routesFor(notification.level());
        if (route == null || route.isEmpty()) {
            log.debug("[alert] no routing for level={} subject={}", notification.level(), notification.subject());
            return;
        }
        for (String channelId : route) {
            String id = channelId.trim().toLowerCase();
            AlertChannel channel = channelsById.get(id);
            if (channel == null) {
                log.warn("[alert] unknown channel id '{}' in routing — skipped", channelId);
                continue;
            }
            try {
                channel.send(notification);
            } catch (Exception ex) {
                log.error("[alert] channel {} failed for subject={}", id, notification.subject(), ex);
            }
        }
    }

    private List<String> routesFor(AlertLevel level) {
        BacpAlertProperties.Routing r = alertProperties.getRouting();
        List<String> list = switch (level) {
            case INFO -> r.getInfo();
            case WARN -> r.getWarn();
            case ERROR -> r.getError();
            case FATAL -> r.getFatal();
        };
        return list != null ? list : Collections.emptyList();
    }
}
