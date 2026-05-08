package io.github.wahhh.bacp.service.risk;

import io.github.wahhh.bacp.common.exception.BizException;
import io.github.wahhh.bacp.config.properties.BacpRiskProperties;
import io.github.wahhh.bacp.mapper.RiskAlertMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskEngineTest {

    @Mock
    private BacpRiskProperties riskProperties;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private RiskAlertMapper riskAlertMapper;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    private RiskEngine riskEngine;

    @BeforeEach
    void createEngine() {
        riskEngine = new RiskEngine(riskProperties, stringRedisTemplate, riskAlertMapper, meterRegistry);
    }

    /**
     * 场景：单笔金额未超过大额阈值且窗口内次数未超限，应放行且不写入告警。
     */
    @Test
    void validateWithdrawAllowsWhenUnderThresholds() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(riskProperties.getLargeAmountThreshold()).thenReturn(new BigDecimal("10000"));
        when(riskProperties.getFrequencyWindowSeconds()).thenReturn(60);
        when(riskProperties.getFrequencyMaxCount()).thenReturn(20);
        when(valueOperations.increment(any())).thenReturn(5L);
        when(stringRedisTemplate.expire(any(), any(Duration.class))).thenReturn(true);

        assertDoesNotThrow(() -> riskEngine.validateWithdraw(42L, new BigDecimal("100")));

        verify(riskAlertMapper, never()).insert(any());
        verify(valueOperations).increment(eq("bacp:risk:withdraw:42"));
        verify(stringRedisTemplate).expire(eq("bacp:risk:withdraw:42"), any(Duration.class));
    }

    /**
     * 场景：频控计数等于配置上限（非大于），边界上应放行。
     */
    @Test
    void validateWithdrawAllowsWhenCountEqualsFrequencyCap() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(riskProperties.getLargeAmountThreshold()).thenReturn(new BigDecimal("999999"));
        when(riskProperties.getFrequencyWindowSeconds()).thenReturn(60);
        when(riskProperties.getFrequencyMaxCount()).thenReturn(3);
        when(valueOperations.increment(any())).thenReturn(3L);
        when(stringRedisTemplate.expire(any(), any(Duration.class))).thenReturn(true);

        assertDoesNotThrow(() -> riskEngine.validateWithdraw(99L, BigDecimal.ONE));

        verify(riskAlertMapper, never()).insert(any());
    }

    /**
     * 场景：单笔超过大额阈值，应拦截、落库告警并打点。
     */
    @Test
    void blocksLargeAmount() {
        when(riskProperties.getLargeAmountThreshold()).thenReturn(new BigDecimal("100"));
        assertThrows(BizException.class, () -> riskEngine.validateWithdraw(1L, new BigDecimal("101")));
        verify(riskAlertMapper).insert(any());
    }

    /**
     * 场景：窗口内提现次数超过上限，应拦截并记录频控告警。
     */
    @Test
    void blocksHighFrequency() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(riskProperties.getLargeAmountThreshold()).thenReturn(new BigDecimal("999999"));
        when(riskProperties.getFrequencyWindowSeconds()).thenReturn(60);
        when(riskProperties.getFrequencyMaxCount()).thenReturn(3);
        when(valueOperations.increment(any())).thenReturn(4L);
        when(stringRedisTemplate.expire(any(), any(Duration.class))).thenReturn(true);

        assertThrows(BizException.class, () -> riskEngine.validateWithdraw(2L, BigDecimal.ONE));
        verify(riskAlertMapper).insert(any());
    }

    /**
     * 场景：频控窗口秒数配置过小（含 0）时，expire 至少按 1 秒设置。
     */
    @Test
    void expireUsesAtLeastOneSecondWhenWindowMisconfigured() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(riskProperties.getLargeAmountThreshold()).thenReturn(new BigDecimal("999999"));
        when(riskProperties.getFrequencyWindowSeconds()).thenReturn(0);
        when(riskProperties.getFrequencyMaxCount()).thenReturn(10);
        when(valueOperations.increment(any())).thenReturn(1L);
        when(stringRedisTemplate.expire(any(), any(Duration.class))).thenReturn(true);

        riskEngine.validateWithdraw(7L, BigDecimal.ONE);

        verify(stringRedisTemplate).expire(any(), eq(Duration.ofSeconds(1)));
    }
}
