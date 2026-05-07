package io.github.wahhh.bacp.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Micrometer timed aspect for {@code @Timed} support on beans.
 */
@Configuration
public class PrometheusConfig {

    /**
     * Enables {@link TimedAspect} for Micrometer.
     *
     * @param registry meter registry
     * @return timed aspect bean
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}
