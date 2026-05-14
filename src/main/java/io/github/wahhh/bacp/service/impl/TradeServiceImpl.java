package io.github.wahhh.bacp.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.wahhh.bacp.entity.TradeExecution;
import io.github.wahhh.bacp.entity.TradeOrder;
import io.github.wahhh.bacp.mapper.TradeExecutionMapper;
import io.github.wahhh.bacp.mapper.TradeOrderMapper;
import io.github.wahhh.bacp.dto.request.OrderCreateRequest;
import io.github.wahhh.bacp.service.TradeService;
import io.github.wahhh.bacp.service.trade.MatchingEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spot trading facade delegating matching to {@link MatchingEngine} while keeping read APIs here.
 */
@Service
@RequiredArgsConstructor
public class TradeServiceImpl implements TradeService {

    private final TradeOrderMapper tradeOrderMapper;

    private final TradeExecutionMapper tradeExecutionMapper;

    private final MatchingEngine matchingEngine;

    /**
     * {@inheritDoc}
     */
    @Override
    public TradeOrder place(Long userId, OrderCreateRequest request) {
        return matchingEngine.placeOrder(userId, request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancel(Long userId, Long orderId) {
        matchingEngine.cancelOrder(userId, orderId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<TradeOrder> listOrders(Long userId, String symbol) {
        var q = Wrappers.<TradeOrder>lambdaQuery()
                .eq(userId != null, TradeOrder::getUserId, userId)
                .eq(symbol != null && !symbol.isBlank(), TradeOrder::getSymbol, symbol)
                .orderByDesc(TradeOrder::getId);
        return tradeOrderMapper.selectList(q);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<TradeExecution> listTrades(String symbol) {
        var q = Wrappers.<TradeExecution>lambdaQuery()
                .eq(symbol != null && !symbol.isBlank(), TradeExecution::getSymbol, symbol)
                .orderByDesc(TradeExecution::getId)
                .last("limit 100");
        return tradeExecutionMapper.selectList(q);
    }
}
