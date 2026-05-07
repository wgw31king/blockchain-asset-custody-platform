package io.github.wahhh.bacp.service.risk;

import io.github.wahhh.bacp.common.exception.BizException;
import io.github.wahhh.bacp.common.result.ResultCode;
import io.github.wahhh.bacp.config.properties.BacpRiskProperties;
import io.github.wahhh.bacp.entity.RiskAlert;
import io.github.wahhh.bacp.mapper.RiskAlertMapper;
import io.github.wahhh.bacp.common.util.JsonUtil;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

/**
 * Lightweight withdraw anomaly checks (amount + velocity).
 */
@Service
@RequiredArgsConstructor
public class RiskEngine {

    private final BacpRiskProperties riskProperties;

    private final StringRedisTemplate stringRedisTemplate;

    private final RiskAlertMapper riskAlertMapper;

    private final MeterRegistry meterRegistry;

    /**
     * Validates withdraw against configured thresholds.
     *
     * @param userId user id
     * @param amount withdraw amount
     */
    public void validateWithdraw(Long userId, BigDecimal amount) {
        if (amount.compareTo(riskProperties.getLargeAmountThreshold()) > 0) {
            recordAlert("LARGE_AMOUNT", userId, Map.of("amount", amount.toPlainString()));
            meterRegistry.counter("bacp_risk_alert_total", "rule", "LARGE_AMOUNT").increment();
            throw new BizException(ResultCode.RISK_BLOCKED, "large amount requires manual review");
        }
        String key = "bacp:risk:withdraw:" + userId;
        Long cnt = stringRedisTemplate.opsForValue().increment(key);
        stringRedisTemplate.expire(key, Duration.ofSeconds(Math.max(riskProperties.getFrequencyWindowSeconds(), 1)));
        if (cnt != null && cnt > riskProperties.getFrequencyMaxCount()) {
            recordAlert("FREQUENCY", userId, Map.of("count", String.valueOf(cnt)));
            meterRegistry.counter("bacp_risk_alert_total", "rule", "FREQUENCY").increment();
            throw new BizException(ResultCode.RISK_BLOCKED, "withdraw frequency too high");
        }
    }

    private void recordAlert(String rule, Long userId, Map<String, String> payload) {
        RiskAlert alert = new RiskAlert();
        alert.setRuleCode(rule);
        alert.setUserId(userId);
        alert.setPayloadJson(JsonUtil.toJson(payload));
        alert.setStatus("OPEN");
        riskAlertMapper.insert(alert);
    }
}
