package io.github.wahhh.bacp.monitor.metrics;

import io.github.wahhh.bacp.config.properties.BacpMetricsProperties;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Counts JDBC-backed mapper executions and records slow statements — Prometheus:
 * {@code bacp_sql_queries_total{mapper_id}}, {@code bacp_sql_slow_total}, {@code bacp_sql_slow_seconds}.
 */
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class,
                        CacheKey.class, BoundSql.class})
})
@RequiredArgsConstructor
public class SqlMetricsInterceptor implements Interceptor {

    private final MeterRegistry meterRegistry;

    private final BacpMetricsProperties metricsProperties;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        String mapperId = ms.getId();
        long startNs = System.nanoTime();
        try {
            return invocation.proceed();
        } finally {
            long elapsedNs = System.nanoTime() - startNs;
            meterRegistry.counter("bacp_sql_queries_total", "mapper_id", mapperId).increment();
            long thresholdNs = TimeUnit.MILLISECONDS.toNanos(metricsProperties.getSqlSlowThresholdMs());
            if (elapsedNs >= thresholdNs) {
                meterRegistry.counter("bacp_sql_slow_total", "mapper_id", mapperId).increment();
                meterRegistry.timer("bacp_sql_slow_seconds", "mapper_id", mapperId)
                        .record(elapsedNs, TimeUnit.NANOSECONDS);
            }
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // unused — wired via constructor
    }
}
