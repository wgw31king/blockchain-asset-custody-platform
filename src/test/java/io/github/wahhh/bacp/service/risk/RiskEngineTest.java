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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

    @Test
    void blocksLargeAmount() {
        when(riskProperties.getLargeAmountThreshold()).thenReturn(new BigDecimal("100"));
        assertThrows(BizException.class, () -> riskEngine.validateWithdraw(1L, new BigDecimal("101")));
        verify(riskAlertMapper).insert(any());
    }

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
}
