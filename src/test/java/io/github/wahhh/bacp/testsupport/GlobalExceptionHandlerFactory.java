package io.github.wahhh.bacp.testsupport;

import io.github.wahhh.bacp.common.exception.GlobalExceptionHandler;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Builds {@link GlobalExceptionHandler} for standalone MockMvc controller tests.
 */
public final class GlobalExceptionHandlerFactory {

    private GlobalExceptionHandlerFactory() {
    }

    /**
     * @return handler wired with a simple in-memory registry
     */
    public static GlobalExceptionHandler create() {
        ObjectProvider<MeterRegistry> provider = Mockito.mock(ObjectProvider.class);
        Mockito.when(provider.getIfAvailable()).thenReturn(new SimpleMeterRegistry());
        return new GlobalExceptionHandler(provider);
    }
}
