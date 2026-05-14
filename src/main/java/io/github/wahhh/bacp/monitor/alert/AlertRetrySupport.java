package io.github.wahhh.bacp.monitor.alert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Retries alert IO operations with fixed delay (SMTP / webhook).
 */
public final class AlertRetrySupport {

    private static final Logger log = LoggerFactory.getLogger(AlertRetrySupport.class);

    public static final int MAX_ATTEMPTS = 3;

    public static final long INTERVAL_MS = 5000L;

    private AlertRetrySupport() {
    }

    /**
     * Runs the runnable up to {@link #MAX_ATTEMPTS} times; waits {@link #INTERVAL_MS} between failures.
     *
     * @param operation description for logs
     * @param runnable    action that may throw
     */
    public static void runWithRetry(String operation, IoRunnable runnable) {
        Exception last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                runnable.run();
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[alert-retry] interrupted during {}", operation);
                return;
            } catch (Exception e) {
                last = e;
                log.warn("[alert-retry] {} attempt {}/{} failed: {}", operation, attempt, MAX_ATTEMPTS, e.getMessage());
                if (attempt < MAX_ATTEMPTS) {
                    try {
                        Thread.sleep(INTERVAL_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("[alert-retry] interrupted sleeping after {}", operation);
                        return;
                    }
                }
            }
        }
        if (last != null) {
            log.error("[alert-retry] {} exhausted after {} attempts", operation, MAX_ATTEMPTS, last);
        }
    }

    @FunctionalInterface
    public interface IoRunnable {
        void run() throws Exception;
    }
}
