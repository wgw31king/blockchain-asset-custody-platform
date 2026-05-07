package io.github.wahhh.bacp.service.impl;

import io.github.wahhh.bacp.common.enums.OrderSide;
import io.github.wahhh.bacp.common.enums.OrderStatus;
import io.github.wahhh.bacp.common.exception.BizException;
import io.github.wahhh.bacp.dto.request.OrderCreateRequest;
import io.github.wahhh.bacp.entity.TradeOrder;
import io.github.wahhh.bacp.mapper.TradeExecutionMapper;
import io.github.wahhh.bacp.mapper.TradeOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeServiceImplTest {

    @Mock
    private TradeOrderMapper tradeOrderMapper;

    @Mock
    private TradeExecutionMapper tradeExecutionMapper;

    @InjectMocks
    private TradeServiceImpl tradeService;

    private OrderCreateRequest limitBuy;

    @BeforeEach
    void setUp() {
        limitBuy = new OrderCreateRequest();
        limitBuy.setSymbol("ETH-USDT");
        limitBuy.setSide(OrderSide.BUY.name());
        limitBuy.setOrderType("LIMIT");
        limitBuy.setPrice(new BigDecimal("2000"));
        limitBuy.setQuantity(new BigDecimal("1"));
    }

    @Test
    void placeRejectsNonLimit() {
        limitBuy.setOrderType("MARKET");
        assertThrows(BizException.class, () -> tradeService.place(1L, limitBuy));
    }

    @Test
    void placeRejectsBadPrice() {
        limitBuy.setPrice(BigDecimal.ZERO);
        assertThrows(BizException.class, () -> tradeService.place(1L, limitBuy));
    }

    @Test
    void placeInsertsAndReturnsLatestRow() {
        when(tradeOrderMapper.insert(any(TradeOrder.class))).thenAnswer(inv -> {
            TradeOrder o = inv.getArgument(0);
            o.setId(42L);
            return 1;
        });
        when(tradeOrderMapper.selectList(any())).thenReturn(List.of());
        TradeOrder saved = new TradeOrder();
        saved.setId(42L);
        saved.setStatus(OrderStatus.OPEN.name());
        when(tradeOrderMapper.selectById(42L)).thenReturn(saved);

        TradeOrder out = tradeService.place(7L, limitBuy);
        assertEquals(42L, out.getId());

        ArgumentCaptor<TradeOrder> cap = ArgumentCaptor.forClass(TradeOrder.class);
        verify(tradeOrderMapper).insert(cap.capture());
        assertEquals("ETH-USDT", cap.getValue().getSymbol());
    }

    @Test
    void cancelNotFound() {
        when(tradeOrderMapper.selectById(9L)).thenReturn(null);
        assertThrows(BizException.class, () -> tradeService.cancel(1L, 9L));
    }
}
