package io.github.wahhh.bacp.service.impl;

import io.github.wahhh.bacp.dto.request.OrderCreateRequest;
import io.github.wahhh.bacp.entity.TradeExecution;
import io.github.wahhh.bacp.entity.TradeOrder;
import io.github.wahhh.bacp.mapper.TradeExecutionMapper;
import io.github.wahhh.bacp.mapper.TradeOrderMapper;
import io.github.wahhh.bacp.service.trade.MatchingEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeServiceImplTest {

    @Mock
    private TradeOrderMapper tradeOrderMapper;

    @Mock
    private TradeExecutionMapper tradeExecutionMapper;

    @Mock
    private MatchingEngine matchingEngine;

    @InjectMocks
    private TradeServiceImpl tradeService;

    @Test
    void placeDelegatesToMatchingEngine() {
        OrderCreateRequest req = new OrderCreateRequest();
        TradeOrder expected = new TradeOrder();
        when(matchingEngine.placeOrder(3L, req)).thenReturn(expected);

        TradeOrder out = tradeService.place(3L, req);

        assertSame(expected, out);
        verify(matchingEngine).placeOrder(3L, req);
    }

    @Test
    void cancelDelegatesToMatchingEngine() {
        tradeService.cancel(9L, 100L);
        verify(matchingEngine).cancelOrder(9L, 100L);
    }

    @Test
    void listOrdersOmitsPredicateWhenFiltersBlank() {
        when(tradeOrderMapper.selectList(any())).thenReturn(new ArrayList<>());

        tradeService.listOrders(null, "   ");

        verify(tradeOrderMapper).selectList(any());
    }

    @Test
    void listTradesWithoutSymbol() {
        when(tradeExecutionMapper.selectList(any())).thenReturn(List.of());

        List<TradeExecution> rows = tradeService.listTrades(null);

        org.junit.jupiter.api.Assertions.assertTrue(rows.isEmpty());
        verify(tradeExecutionMapper).selectList(any());
    }
}
