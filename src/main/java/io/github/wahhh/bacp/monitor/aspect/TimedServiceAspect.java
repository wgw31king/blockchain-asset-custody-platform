package io.github.wahhh.bacp.monitor.aspect;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Records wall-clock latency for all Spring {@code @Service} beans into Micrometer.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class TimedServiceAspect {

    private final MeterRegistry meterRegistry;

    /**
     * Times service-layer methods for Grafana latency panels.
     *
     * @param pjp join point
     * @return advised method result
     * @throws Throwable propagated
     */
    @Around("@within(org.springframework.stereotype.Service)")
    public Object aroundService(ProceedingJoinPoint pjp) throws Throwable {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return pjp.proceed();
        } finally {
            sample.stop(Timer.builder("bacp_service_seconds")
                    .tag("class", pjp.getTarget().getClass().getSimpleName())
                    .tag("method", pjp.getSignature().getName())
                    .register(meterRegistry));
        }
    }
}
