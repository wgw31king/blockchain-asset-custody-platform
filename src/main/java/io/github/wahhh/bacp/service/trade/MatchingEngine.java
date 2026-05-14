package io.github.wahhh.bacp.service.trade;

import io.github.wahhh.bacp.common.constant.CacheKeys;
import io.github.wahhh.bacp.common.exception.BizException;
import io.github.wahhh.bacp.common.result.ResultCode;
import io.github.wahhh.bacp.config.properties.BacpTradeProperties;
import io.github.wahhh.bacp.dto.request.OrderCreateRequest;
import io.github.wahhh.bacp.entity.TradeOrder;
import io.github.wahhh.bacp.mapper.TradeOrderMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Distributed entry to the matcher: acquires a per-symbol {@link RLock} before delegating to
 * {@link MatchingTransactionService} so concurrent placements cannot double-fill the same resting
 * order.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingEngine {

    private final RedissonClient redissonClient;

    private final MatchingTransactionService matchingTransactionService;

    private final TradeOrderMapper tradeOrderMapper;

    private final BacpTradeProperties tradeProperties;

    private final MeterRegistry meterRegistry;

    /**
     * Places the taker order under the symbol lock and runs the transactional match pipeline.
     *
     * @param userId  authenticated trader
     * @param request validated REST payload
     * @return persisted order row after matching
     */
    public TradeOrder placeOrder(Long userId, OrderCreateRequest request) {
        String symbol = request.getSymbol().trim().toUpperCase();
        RLock lock = redissonClient.getLock(CacheKeys.TRADE_SYMBOL_LOCK + symbol);
        try {
            if (!lock.tryLock(tradeProperties.getLockWaitMs(), tradeProperties.getLockLeaseMs(), TimeUnit.MILLISECONDS)) {
                meterRegistry.counter("bacp_trade_lock_fail_total").increment();
                throw new BizException(ResultCode.CONFLICT, "symbol matching busy");
            }
            try {
                return matchingTransactionService.placeAndMatch(userId, request);
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException(ResultCode.INTERNAL_ERROR, "interrupted while acquiring trade lock");
        }
    }

    /**
     * Cancels a resting order for the owner under the same symbol lock used for matching.
     */
    public void cancelOrder(Long userId, Long orderId) {
        TradeOrder row = tradeOrderMapper.selectById(orderId);
        if (row == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        withSymbolLock(row.getSymbol().trim().toUpperCase(),
                () -> matchingTransactionService.cancelByUser(userId, orderId));
    }

    /**
     * System expiry path: reuses the symbol lock so expiry cannot race an in-flight match on the
     * same pair.
     */
    public void cancelExpiredOrder(Long orderId) {
        TradeOrder row = tradeOrderMapper.selectById(orderId);
        if (row == null) {
            return;
        }
        withSymbolLock(row.getSymbol().trim().toUpperCase(),
                () -> matchingTransactionService.cancelExpired(orderId));
    }

    private void withSymbolLock(String symbol, Runnable action) {
        RLock lock = redissonClient.getLock(CacheKeys.TRADE_SYMBOL_LOCK + symbol);
        try {
            if (!lock.tryLock(tradeProperties.getLockWaitMs(), tradeProperties.getLockLeaseMs(), TimeUnit.MILLISECONDS)) {
                meterRegistry.counter("bacp_trade_lock_fail_total").increment();
                throw new BizException(ResultCode.CONFLICT, "symbol matching busy");
            }
            try {
                action.run();
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException(ResultCode.INTERNAL_ERROR, "interrupted while acquiring trade lock");
        }
    }
}
