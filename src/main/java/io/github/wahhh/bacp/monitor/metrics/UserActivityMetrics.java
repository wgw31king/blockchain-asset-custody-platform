package io.github.wahhh.bacp.monitor.metrics;

import io.github.wahhh.bacp.common.constant.CacheKeys;
import io.github.wahhh.bacp.config.properties.BacpMetricsProperties;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Exports {@code bacp_user_active_daily_approx{window="daily"}} from Redis HyperLogLog populated on login.
 */
@Component
@ConditionalOnBean(StringRedisTemplate.class)
@RequiredArgsConstructor
public class UserActivityMetrics {

    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;

    private final StringRedisTemplate stringRedisTemplate;

    private final MeterRegistry meterRegistry;

    private final BacpMetricsProperties metricsProperties;

    private final AtomicReference<Double> dailyApproxHolder = new AtomicReference<>(0.0);

    /**
     * Registers gauge once; value refreshed on schedule and after each login touch.
     */
    @PostConstruct
    void registerGauge() {
        Gauge.builder("bacp_user_active_daily_approx", dailyApproxHolder, AtomicReference::get)
                .tag("window", "daily")
                .description("Approximate distinct users with successful login today (Redis PFCOUNT on HyperLogLog)")
                .register(meterRegistry);
        refreshGaugeForToday();
    }

    /**
     * Records user id in today's DAU set — Prometheus: PFADD side effect only; gauge updates asynchronously.
     *
     * @param userId authenticated user
     */
    public void recordSuccessfulLogin(Long userId) {
        if (userId == null) {
            return;
        }
        String key = CacheKeys.METRICS_DAU_PREFIX + LocalDate.now().format(DAY);
        stringRedisTemplate.opsForHyperLogLog().add(key, userId.toString());
        stringRedisTemplate.expire(key, Duration.ofHours(metricsProperties.getDauKeyTtlHours()));
        refreshGaugeForKey(key);
    }

    @Scheduled(fixedDelayString = "${bacp.metrics.dau-refresh-ms:60000}")
    void scheduledRefresh() {
        refreshGaugeForToday();
    }

    private void refreshGaugeForToday() {
        refreshGaugeForKey(CacheKeys.METRICS_DAU_PREFIX + LocalDate.now().format(DAY));
    }

    private void refreshGaugeForKey(String key) {
        Long size = stringRedisTemplate.opsForHyperLogLog().size(key);
        dailyApproxHolder.set(size != null ? size.doubleValue() : 0.0);
    }
}
