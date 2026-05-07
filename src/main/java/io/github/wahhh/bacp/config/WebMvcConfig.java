package io.github.wahhh.bacp.config;

import io.github.wahhh.bacp.monitor.web.ApiMetricsInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers cross-cutting MVC interceptors.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ApiMetricsInterceptor apiMetricsInterceptor;

    /**
     * @param apiMetricsInterceptor HTTP metrics recorder
     */
    public WebMvcConfig(ApiMetricsInterceptor apiMetricsInterceptor) {
        this.apiMetricsInterceptor = apiMetricsInterceptor;
    }

    /**
     * Adds {@link ApiMetricsInterceptor} for API routes.
     *
     * @param registry interceptor registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiMetricsInterceptor).addPathPatterns("/api/**");
    }
}
