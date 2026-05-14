package io.github.wahhh.bacp.monitor.alert;

import io.github.wahhh.bacp.config.properties.BacpAlertProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding-window rate limit for alert dispatch (in-memory).
 */
@Component
public class AlertThrottle {

    private final BacpAlertProperties alertProperties;

    private final ConcurrentHashMap<String, Deque<Long>> buckets = new ConcurrentHashMap<>();

    public AlertThrottle(BacpAlertProperties alertProperties) {
        this.alertProperties = alertProperties;
    }

    /**
     * @param throttleKey dedupe key for this alert stream
     * @return true if send is allowed
     */
    public boolean tryAcquire(String throttleKey) {
        BacpAlertProperties.Throttle t = alertProperties.getThrottle();
        if (!t.isEnabled()) {
            return true;
        }
        if (throttleKey == null || throttleKey.isBlank()) {
            return true;
        }
        long windowMs = Math.max(t.getWindowSeconds(), 1) * 1000L;
        int max = Math.max(t.getMaxPerWindow(), 1);
        long now = System.currentTimeMillis();
        Deque<Long> dq = buckets.computeIfAbsent(throttleKey, k -> new ArrayDeque<>());
        synchronized (dq) {
            while (!dq.isEmpty() && now - dq.peekFirst() >= windowMs) {
                dq.pollFirst();
            }
            if (dq.size() >= max) {
                return false;
            }
            dq.addLast(now);
            return true;
        }
    }
}
