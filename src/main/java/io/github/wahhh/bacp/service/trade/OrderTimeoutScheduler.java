package io.github.wahhh.bacp.service.trade;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.wahhh.bacp.common.enums.OrderStatus;
import io.github.wahhh.bacp.entity.TradeOrder;
import io.github.wahhh.bacp.mapper.TradeOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scans for expired resting orders and routes them through {@link MatchingEngine} so reservations
 * are released under the same distributed lock as user-driven cancels.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.task.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class OrderTimeoutScheduler {

    private final TradeOrderMapper tradeOrderMapper;

    private final MatchingEngine matchingEngine;

    /**
     * Polls a small batch of expired open orders. Delay is configurable via {@code bacp.trade.timeout-poll-ms}.
     */
    @Scheduled(fixedDelayString = "${bacp.trade.timeout-poll-ms:30000}")
    public void sweepExpiredOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<TradeOrder> batch = tradeOrderMapper.selectList(Wrappers.<TradeOrder>lambdaQuery()
                .in(TradeOrder::getStatus, OrderStatus.PENDING.name(), OrderStatus.PARTIALLY_FILLED.name())
                .isNotNull(TradeOrder::getExpiresAt)
                .lt(TradeOrder::getExpiresAt, now)
                .last("LIMIT 50"));
        for (TradeOrder o : batch) {
            try {
                matchingEngine.cancelExpiredOrder(o.getId());
            } catch (Exception ex) {
                log.warn("expire cancel failed orderId={} msg={}", o.getId(), ex.getMessage());
            }
        }
    }
}
