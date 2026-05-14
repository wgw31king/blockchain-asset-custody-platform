package io.github.wahhh.bacp.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import io.github.wahhh.bacp.config.properties.BacpMetricsProperties;
import io.github.wahhh.bacp.monitor.metrics.SqlMetricsInterceptor;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus pagination, optimistic lock, and audit field filling.
 */
@Configuration
public class MyBatisPlusConfig {

    /**
     * Registers global interceptors.
     *
     * @return interceptor chain
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }

    /**
     * MyBatis layer timings — wires into Spring Boot's SqlSessionFactory alongside MP inner interceptors.
     *
     * @param meterRegistry registry
     * @param props slow-query threshold from bacp.metrics.sql-slow-threshold-ms
     * @return plugin
     */
    @Bean
    public SqlMetricsInterceptor sqlMetricsInterceptor(MeterRegistry meterRegistry, BacpMetricsProperties props) {
        return new SqlMetricsInterceptor(meterRegistry, props);
    }

    /**
     * Fills {@code createdAt} / {@code updatedAt} on insert/update.
     *
     * @return meta object handler
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                LocalDateTime now = LocalDateTime.now();
                strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
                strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
