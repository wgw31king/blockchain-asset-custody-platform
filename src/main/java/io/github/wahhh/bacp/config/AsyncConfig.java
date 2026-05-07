package io.github.wahhh.bacp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async executor for background tasks (alerts, metrics hooks).
 */
@Configuration
public class AsyncConfig {

    /**
     * Shared async executor.
     *
     * @return task executor
     */
    @Bean(name = "bacpAsyncExecutor")
    public Executor bacpAsyncExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(4);
        ex.setMaxPoolSize(16);
        ex.setQueueCapacity(500);
        ex.setThreadNamePrefix("bacp-async-");
        ex.initialize();
        return ex;
    }
}
