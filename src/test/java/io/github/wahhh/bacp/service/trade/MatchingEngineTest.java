package io.github.wahhh.bacp.service.trade;

import io.github.wahhh.bacp.common.exception.BizException;
import io.github.wahhh.bacp.common.result.ResultCode;
import io.github.wahhh.bacp.config.properties.BacpTradeProperties;
import io.github.wahhh.bacp.dto.request.OrderCreateRequest;
import io.github.wahhh.bacp.entity.TradeOrder;
import io.github.wahhh.bacp.mapper.TradeOrderMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchingEngineTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private MatchingTransactionService matchingTransactionService;

    @Mock
    private TradeOrderMapper tradeOrderMapper;

    @Mock
    private BacpTradeProperties tradeProperties;

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    @InjectMocks
    private MatchingEngine matchingEngine;

    @Mock
    private RLock rLock;

    @BeforeEach
    void lockStub() throws InterruptedException {
        ReflectionTestUtils.setField(matchingEngine, "meterRegistry", meterRegistry);
        lenient().when(tradeProperties.getLockWaitMs()).thenReturn(50L);
        lenient().when(tradeProperties.getLockLeaseMs()).thenReturn(5000L);
        lenient().when(redissonClient.getLock(anyString())).thenReturn(rLock);
        lenient().when(rLock.tryLock(anyLong(), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(true);
        lenient().when(rLock.isHeldByCurrentThread()).thenReturn(true);
    }

    @Test
    void placeOrderInvokesTransactionServiceUnderLock() {
        OrderCreateRequest req = new OrderCreateRequest();
        req.setSymbol("eth-usdt");
        TradeOrder out = new TradeOrder();
        out.setId(7L);
        when(matchingTransactionService.placeAndMatch(1L, req)).thenReturn(out);

        TradeOrder result = matchingEngine.placeOrder(1L, req);

        assertEquals(7L, result.getId());
        verify(matchingTransactionService).placeAndMatch(1L, req);
        verify(rLock).unlock();
    }

    @Test
    void placeOrderConflictWhenLockUnavailable() throws InterruptedException {
        when(rLock.tryLock(anyLong(), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(false);
        OrderCreateRequest req = new OrderCreateRequest();
        req.setSymbol("ETH-USDT");

        BizException ex = assertThrows(BizException.class, () -> matchingEngine.placeOrder(1L, req));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void cancelOrderLoadsSymbolAndLocks() {
        TradeOrder row = new TradeOrder();
        row.setId(3L);
        row.setSymbol("ETH-USDT");
        when(tradeOrderMapper.selectById(3L)).thenReturn(row);

        matchingEngine.cancelOrder(2L, 3L);

        verify(matchingTransactionService).cancelByUser(2L, 3L);
        verify(rLock).unlock();
    }
}
