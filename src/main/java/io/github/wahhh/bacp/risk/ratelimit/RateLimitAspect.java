package io.github.wahhh.bacp.risk.ratelimit;

import io.github.wahhh.bacp.common.constant.CacheKeys;
import io.github.wahhh.bacp.common.exception.RateLimitException;
import io.github.wahhh.bacp.config.properties.BacpRateLimitProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.List;

/**
 * Applies {@link RateLimit} using a Redis-backed Lua token bucket.
 */
@Aspect
@Component
public class RateLimitAspect {

    private final StringRedisTemplate stringRedisTemplate;

    private final BacpRateLimitProperties rateLimitProperties;

    private final ExpressionParser parser = new SpelExpressionParser();

    private final DefaultRedisScript<Long> tokenBucketScript;

    /**
     * @param stringRedisTemplate Redis executor for Lua script
     * @param rateLimitProperties default bucket tuning when annotation omits values
     */
    public RateLimitAspect(StringRedisTemplate stringRedisTemplate, BacpRateLimitProperties rateLimitProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.rateLimitProperties = rateLimitProperties;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/token_bucket.lua"));
        script.setResultType(Long.class);
        this.tokenBucketScript = script;
    }

    /**
     * Enforces annotated limits before invocation proceeds.
     *
     * @param pjp    join point
     * @param limit  annotation
     * @return proxied result
     * @throws Throwable propagated errors
     */
    @Around("@annotation(limit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit limit) throws Throwable {
        HttpServletRequest request = currentRequest();
        String bucketKey = resolveKey(limit.key(), pjp, request);
        int capacity = limit.capacity() > 0 ? limit.capacity() : rateLimitProperties.getDefaultCapacity();
        double refill = limit.refillPerSec() > 0 ? limit.refillPerSec() : rateLimitProperties.getDefaultRefillPerSec();
        double nowSec = System.currentTimeMillis() / 1000d;
        List<String> keys = Collections.singletonList(CacheKeys.RATE_LIMIT + bucketKey);
        Long allowed = stringRedisTemplate.execute(tokenBucketScript, keys,
                String.valueOf(capacity),
                String.valueOf(refill),
                String.valueOf(nowSec),
                String.valueOf(limit.cost()));
        if (allowed == null || allowed == 0L) {
            throw new RateLimitException();
        }
        return pjp.proceed();
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            return sra.getRequest();
        }
        return null;
    }

    private String resolveKey(String spel, ProceedingJoinPoint pjp, HttpServletRequest request) {
        if (spel != null && !spel.isBlank()) {
            MethodSignature ms = (MethodSignature) pjp.getSignature();
            MethodBasedEvaluationContext ctx = new MethodBasedEvaluationContext(
                    pjp.getTarget(), ms.getMethod(), pjp.getArgs(), new DefaultParameterNameDiscoverer());
            ctx.setVariable("request", request);
            Object value = parser.parseExpression(spel).getValue(ctx);
            if (value != null) {
                return String.valueOf(value);
            }
        }
        if (request != null) {
            return request.getMethod() + ":" + request.getRequestURI();
        }
        return pjp.getSignature().toShortString();
    }
}
