package io.github.wahhh.bacp.monitor.metrics;

import io.github.wahhh.bacp.config.properties.BacpMetricsProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Invocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SqlMetricsInterceptorTest {

    @Mock
    private Executor executor;

    @Mock
    private MappedStatement mappedStatement;

    @Test
    void countsUpdatesAndSlowWhenThresholdZero() throws Throwable {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BacpMetricsProperties props = new BacpMetricsProperties();
        props.setSqlSlowThresholdMs(0L);

        when(mappedStatement.getId()).thenReturn("io.github.wahhh.bacp.mapper.FooMapper.updateById");
        when(executor.update(mappedStatement, null)).thenReturn(1);

        Method method = Executor.class.getMethod("update", MappedStatement.class, Object.class);
        Invocation invocation = new Invocation(executor, method, new Object[]{mappedStatement, null});

        SqlMetricsInterceptor cut = new SqlMetricsInterceptor(registry, props);
        cut.intercept(invocation);

        verify(executor).update(mappedStatement, null);
        assertEquals(1.0,
                registry.counter("bacp_sql_queries_total", "mapper_id", "io.github.wahhh.bacp.mapper.FooMapper.updateById")
                        .count());
        assertEquals(1.0,
                registry.counter("bacp_sql_slow_total", "mapper_id", "io.github.wahhh.bacp.mapper.FooMapper.updateById")
                        .count());
    }
}
