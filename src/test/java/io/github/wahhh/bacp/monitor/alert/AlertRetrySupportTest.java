package io.github.wahhh.bacp.monitor.alert;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AlertRetrySupportTest {

    @Test
    void succeedsAfterTransientFailures() {
        AtomicInteger n = new AtomicInteger();
        AlertRetrySupport.runWithRetry("test-op", () -> {
            if (n.incrementAndGet() < 3) {
                throw new IllegalStateException("transient");
            }
        });
        assertEquals(3, n.get());
    }

    @Test
    void stopsAfterMaxAttempts() {
        AtomicInteger n = new AtomicInteger();
        AlertRetrySupport.runWithRetry("test-op", () -> {
            n.incrementAndGet();
            throw new IllegalStateException("always");
        });
        assertEquals(3, n.get());
    }
}
