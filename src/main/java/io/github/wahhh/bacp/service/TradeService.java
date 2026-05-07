package io.github.wahhh.bacp.service;

import io.github.wahhh.bacp.dto.request.OrderCreateRequest;
import io.github.wahhh.bacp.entity.TradeExecution;
import io.github.wahhh.bacp.entity.TradeOrder;

import java.util.List;

/**
 * Demo spot matching orchestrator.
 */
public interface TradeService {

    /**
     * Places an order and attempts immediate matching against resting liquidity.
     *
     * @param userId  trader id
     * @param request order payload
     * @return persisted order snapshot
     */
    TradeOrder place(Long userId, OrderCreateRequest request);

    /**
     * Cancels an open order owned by the user.
     *
     * @param userId  trader id
     * @param orderId order id
     */
    void cancel(Long userId, Long orderId);

    /**
     * Lists open orders for symbol optional filter.
     *
     * @param userId optional owner filter
     * @param symbol optional symbol filter
     * @return orders
     */
    List<TradeOrder> listOrders(Long userId, String symbol);

    /**
     * Recent executions for optional symbol.
     *
     * @param symbol symbol filter
     * @return trades
     */
    List<TradeExecution> listTrades(String symbol);
}
