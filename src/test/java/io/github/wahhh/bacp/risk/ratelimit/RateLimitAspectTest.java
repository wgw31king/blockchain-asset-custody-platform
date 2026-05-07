package io.github.wahhh.bacp.risk.ratelimit;

import io.github.wahhh.bacp.config.properties.BacpRateLimitProperties;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitAspectTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ProceedingJoinPoint pjp;

    @RateLimit(capacity = 10, refillPerSec = 2, cost = 1, key = "")
    private void gatedHook() {
        // anchor for annotation reflection only
    }

    @AfterEach
    void resetContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void allowsWhenLuaReturnsOne() throws Throwable {
        BacpRateLimitProperties props = new BacpRateLimitProperties();
        RateLimitAspect aspect = new RateLimitAspect(stringRedisTemplate, props);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RateLimit limit = RateLimitAspectTest.class.getDeclaredMethod("gatedHook").getAnnotation(RateLimit.class);

        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(),
                eq("10"), eq("2.0"), anyString(), eq("1"))).thenReturn(1L);
        when(pjp.proceed()).thenReturn("ok");

        assertEquals("ok", aspect.around(pjp, limit));
    }
}
