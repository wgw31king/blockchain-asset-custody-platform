package io.github.wahhh.bacp.monitor.alert;

import io.github.wahhh.bacp.config.properties.BacpAlertProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertThrottleTest {

    @Test
    void allowsUpToMaxPerWindow() {
        BacpAlertProperties p = new BacpAlertProperties();
        p.getThrottle().setEnabled(true);
        p.getThrottle().setWindowSeconds(60);
        p.getThrottle().setMaxPerWindow(3);
        AlertThrottle throttle = new AlertThrottle(p);
        assertTrue(throttle.tryAcquire("key-a"));
        assertTrue(throttle.tryAcquire("key-a"));
        assertTrue(throttle.tryAcquire("key-a"));
        assertFalse(throttle.tryAcquire("key-a"));
    }

    @Test
    void disabledThrottleAlwaysAllows() {
        BacpAlertProperties p = new BacpAlertProperties();
        p.getThrottle().setEnabled(false);
        AlertThrottle throttle = new AlertThrottle(p);
        for (int i = 0; i < 100; i++) {
            assertTrue(throttle.tryAcquire("same-key"));
        }
    }
}
