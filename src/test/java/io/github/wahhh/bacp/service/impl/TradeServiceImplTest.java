package io.github.wahhh.bacp.service.impl;

import io.github.wahhh.bacp.common.enums.OrderSide;
import io.github.wahhh.bacp.common.enums.OrderStatus;
import io.github.wahhh.bacp.common.exception.BizException;
import io.github.wahhh.bacp.dto.request.OrderCreateRequest;
import io.github.wahhh.bacp.entity.TradeExecution;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
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

    /**
     * 场景：仅支持限价单，市价单直接被拒。
     */
    @Test
    void placeRejectsNonLimit() {
        limitBuy.setOrderType("MARKET");
        assertThrows(BizException.class, () -> tradeService.place(1L, limitBuy));
    }

    /**
     * 场景：限价价格为空或不大于 0，校验失败。
     */
    @Test
    void placeRejectsBadPrice() {
        limitBuy.setPrice(BigDecimal.ZERO);
        assertThrows(BizException.class, () -> tradeService.place(1L, limitBuy));
    }

    /**
     * 场景：挂单插入后无对手盘，订单保持 OPEN。
     */
    @Test
    void placeInsertsAndReturnsLatestRow() {
        AtomicReference<TradeOrder> takerRef = new AtomicReference<>();
        when(tradeOrderMapper.insert(any(TradeOrder.class))).thenAnswer(inv -> {
            TradeOrder o = inv.getArgument(0);
            o.setId(42L);
            takerRef.set(o);
            return 1;
        });
        when(tradeOrderMapper.selectList(any())).thenReturn(List.of());
        when(tradeOrderMapper.selectById(42L)).thenAnswer(inv -> takerRef.get());

        TradeOrder out = tradeService.place(7L, limitBuy);
        assertEquals(42L, out.getId());

        ArgumentCaptor<TradeOrder> cap = ArgumentCaptor.forClass(TradeOrder.class);
        verify(tradeOrderMapper).insert(cap.capture());
        assertEquals("ETH-USDT", cap.getValue().getSymbol());
        assertEquals(OrderStatus.OPEN.name(), takerRef.get().getStatus());
    }

    /**
     * 场景：限价买单与更低价卖单撮合，双方成交且状态为 FILLED。
     */
    @Test
    void buyMatchesAgainstLowerPricedSell() {
        TradeOrder maker = sellMaker(2L, 50L, "1900", "1");
        stubPlaceFlow(100L, List.of(maker));

        TradeOrder out = tradeService.place(1L, limitBuy);

        assertEquals(OrderStatus.FILLED.name(), out.getStatus());
        assertEquals(new BigDecimal("1"), out.getFilledQuantity());
        assertEquals(OrderStatus.FILLED.name(), maker.getStatus());
        verify(tradeExecutionMapper).insert(any(TradeExecution.class));
        verify(tradeOrderMapper, atLeastOnce()).updateById(any(TradeOrder.class));
    }

    /**
     * 场景：买单数量大于卖单数量，买单部分成交。
     */
    @Test
    void partialFillLeavesBuyerOpen() {
        limitBuy.setQuantity(new BigDecimal("2"));
        TradeOrder maker = sellMaker(2L, 51L, "1900", "1");
        stubPlaceFlow(101L, List.of(maker));

        TradeOrder out = tradeService.place(1L, limitBuy);

        assertEquals(OrderStatus.PARTIALLY_FILLED.name(), out.getStatus());
        assertEquals(new BigDecimal("1"), out.getFilledQuantity());
        assertEquals(OrderStatus.FILLED.name(), maker.getStatus());
    }

    /**
     * 场景：卖单与更高价买单撮合，成交记录买方为挂单、卖方为吃单。
     */
    @Test
    void sellMatchesAgainstHigherPricedBuy() {
        OrderCreateRequest limitSell = new OrderCreateRequest();
        limitSell.setSymbol("ETH-USDT");
        limitSell.setSide(OrderSide.SELL.name());
        limitSell.setOrderType("LIMIT");
        limitSell.setPrice(new BigDecimal("1800"));
        limitSell.setQuantity(new BigDecimal("1"));

        TradeOrder maker = buyMaker(3L, 60L, "2000", "1");
        stubPlaceFlow(102L, List.of(maker));

        TradeOrder out = tradeService.place(4L, limitSell);

        assertEquals(OrderStatus.FILLED.name(), out.getStatus());
        ArgumentCaptor<TradeExecution> execCap = ArgumentCaptor.forClass(TradeExecution.class);
        verify(tradeExecutionMapper).insert(execCap.capture());
        assertEquals(Long.valueOf(60L), execCap.getValue().getBuyOrderId());
        assertEquals(Long.valueOf(102L), execCap.getValue().getSellOrderId());
    }

    /**
     * 场景：对手盘已全部成交（剩余量为 0），应跳过并尝试下一档。
     */
    @Test
    void skipsMakerWithNoRemainingQuantity() {
        TradeOrder exhausted = sellMaker(2L, 70L, "1900", "1");
        exhausted.setFilledQuantity(new BigDecimal("1"));
        TradeOrder active = sellMaker(2L, 71L, "1950", "1");
        stubPlaceFlow(103L, List.of(exhausted, active));

        TradeOrder out = tradeService.place(1L, limitBuy);

        assertEquals(OrderStatus.FILLED.name(), out.getStatus());
        verify(tradeExecutionMapper).insert(any(TradeExecution.class));
    }

    /**
     * 场景：撤单时订单不存在，返回 NOT_FOUND。
     */
    @Test
    void cancelNotFound() {
        when(tradeOrderMapper.selectById(9L)).thenReturn(null);
        assertThrows(BizException.class, () -> tradeService.cancel(1L, 9L));
    }

    /**
     * 场景：订单归属用户不一致，视为 NOT_FOUND。
     */
    @Test
    void cancelRejectsWrongOwner() {
        TradeOrder order = new TradeOrder();
        order.setId(3L);
        order.setUserId(99L);
        order.setStatus(OrderStatus.OPEN.name());
        when(tradeOrderMapper.selectById(3L)).thenReturn(order);

        assertThrows(BizException.class, () -> tradeService.cancel(1L, 3L));
    }

    /**
     * 场景：已成交订单不允许撤销。
     */
    @Test
    void cancelRejectsFilledOrder() {
        TradeOrder order = new TradeOrder();
        order.setId(4L);
        order.setUserId(1L);
        order.setStatus(OrderStatus.FILLED.name());
        when(tradeOrderMapper.selectById(4L)).thenReturn(order);

        assertThrows(BizException.class, () -> tradeService.cancel(1L, 4L));
    }

    /**
     * 场景：部分成交订单仍可撤销。
     */
    @Test
    void cancelPartiallyFilledOrderSucceeds() {
        TradeOrder order = new TradeOrder();
        order.setId(6L);
        order.setUserId(1L);
        order.setStatus(OrderStatus.PARTIALLY_FILLED.name());
        when(tradeOrderMapper.selectById(6L)).thenReturn(order);

        tradeService.cancel(1L, 6L);

        assertEquals(OrderStatus.CANCELED.name(), order.getStatus());
        verify(tradeOrderMapper).updateById(order);
    }

    /**
     * 场景：OPEN / PARTIALLY_FILLED 可撤单并更新为 CANCELED。
     */
    @Test
    void cancelOpenOrderSucceeds() {
        TradeOrder order = new TradeOrder();
        order.setId(5L);
        order.setUserId(1L);
        order.setStatus(OrderStatus.OPEN.name());
        when(tradeOrderMapper.selectById(5L)).thenReturn(order);

        tradeService.cancel(1L, 5L);

        assertEquals(OrderStatus.CANCELED.name(), order.getStatus());
        verify(tradeOrderMapper).updateById(order);
    }

    /**
     * 场景：listOrders 在 userId 为 null 时不按用户过滤；symbol 为空白时不按交易对过滤。
     */
    @Test
    void listOrdersOmitsPredicateWhenFiltersBlank() {
        when(tradeOrderMapper.selectList(any())).thenReturn(new ArrayList<>());

        tradeService.listOrders(null, "   ");

        verify(tradeOrderMapper).selectList(any());
    }

    /**
     * 场景：listTrades 在 symbol 为空时返回近期成交列表查询。
     */
    @Test
    void listTradesWithoutSymbol() {
        when(tradeExecutionMapper.selectList(any())).thenReturn(List.of());

        List<TradeExecution> rows = tradeService.listTrades(null);

        assertTrue(rows.isEmpty());
        verify(tradeExecutionMapper).selectList(any());
    }

    private void stubPlaceFlow(long takerId, List<TradeOrder> makers) {
        AtomicReference<TradeOrder> takerRef = new AtomicReference<>();
        when(tradeOrderMapper.insert(any(TradeOrder.class))).thenAnswer(inv -> {
            TradeOrder o = inv.getArgument(0);
            o.setId(takerId);
            takerRef.set(o);
            return 1;
        });
        when(tradeOrderMapper.selectList(any())).thenReturn(makers);
        when(tradeOrderMapper.selectById(takerId)).thenAnswer(inv -> takerRef.get());
    }

    private static TradeOrder sellMaker(Long userId, Long orderId, String price, String qty) {
        TradeOrder maker = new TradeOrder();
        maker.setId(orderId);
        maker.setUserId(userId);
        maker.setSymbol("ETH-USDT");
        maker.setSide(OrderSide.SELL.name());
        maker.setPrice(new BigDecimal(price));
        maker.setQuantity(new BigDecimal(qty));
        maker.setFilledQuantity(BigDecimal.ZERO);
        maker.setStatus(OrderStatus.OPEN.name());
        return maker;
    }

    private static TradeOrder buyMaker(Long userId, Long orderId, String price, String qty) {
        TradeOrder maker = new TradeOrder();
        maker.setId(orderId);
        maker.setUserId(userId);
        maker.setSymbol("ETH-USDT");
        maker.setSide(OrderSide.BUY.name());
        maker.setPrice(new BigDecimal(price));
        maker.setQuantity(new BigDecimal(qty));
        maker.setFilledQuantity(BigDecimal.ZERO);
        maker.setStatus(OrderStatus.OPEN.name());
        return maker;
    }
}
