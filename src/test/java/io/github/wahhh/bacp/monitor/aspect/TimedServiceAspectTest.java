package io.github.wahhh.bacp.monitor.aspect;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TimedServiceAspectTest {

    @Test
    void recordsTimerOnSuccess() throws Throwable {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        TimedServiceAspect aspect = new TimedServiceAspect(registry);

        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        MethodSignature sig = mock(MethodSignature.class);
        when(pjp.getSignature()).thenReturn(sig);
        when(sig.getName()).thenReturn("demo");
        when(pjp.getTarget()).thenReturn(new Object());
        when(pjp.proceed()).thenReturn("ok");

        assertEquals("ok", aspect.aroundService(pjp));
        assertEquals(1, registry.get("bacp_service_seconds").timer().count());
    }
}
