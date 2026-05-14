package io.github.wahhh.bacp.monitor.metrics;

import io.github.wahhh.bacp.config.properties.BacpMetricsProperties;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Exposes {@code bacp_disk_free_bytes{path}} from {@link java.nio.file.FileStore#getUsableSpace()} for OS/container root.
 */
@Slf4j
@Component("bacpDiskFreeMetrics")
@RequiredArgsConstructor
public class BacpDiskFreeMetrics {

    private final BacpMetricsProperties metricsProperties;

    private final MeterRegistry meterRegistry;

    private final AtomicLong usableBytes = new AtomicLong(-1L);

    /**
     * Registers gauge reference once at startup.
     */
    @PostConstruct
    void registerGauge() {
        Gauge.builder("bacp_disk_free_bytes", usableBytes, AtomicLong::get)
                .tag("path", metricsProperties.getDiskFreePath())
                .description("Usable space at monitored path (often container root)")
                .register(meterRegistry);
        refresh();
    }

    /**
     * Polls disk periodically; failures publish {@code -1} so dashboards can alert on invalid readings.
     */
    @Scheduled(fixedDelayString = "${bacp.metrics.disk-poll-ms:60000}")
    void refresh() {
        Path path = Path.of(metricsProperties.getDiskFreePath());
        try {
            long usable = Files.getFileStore(path).getUsableSpace();
            usableBytes.set(usable);
        } catch (IOException ex) {
            log.debug("disk metrics poll failed path={} msg={}", path, ex.getMessage());
            usableBytes.set(-1L);
        }
    }
}
