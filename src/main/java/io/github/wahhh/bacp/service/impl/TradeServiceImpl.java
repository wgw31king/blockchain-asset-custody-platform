package io.github.wahhh.bacp.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.wahhh.bacp.common.enums.OrderSide;
import io.github.wahhh.bacp.common.enums.OrderStatus;
import io.github.wahhh.bacp.common.enums.OrderType;
import io.github.wahhh.bacp.common.exception.BizException;
import io.github.wahhh.bacp.common.result.ResultCode;
import io.github.wahhh.bacp.dto.request.OrderCreateRequest;
import io.github.wahhh.bacp.entity.TradeExecution;
import io.github.wahhh.bacp.entity.TradeOrder;
import io.github.wahhh.bacp.mapper.TradeExecutionMapper;
import io.github.wahhh.bacp.mapper.TradeOrderMapper;
import io.github.wahhh.bacp.service.TradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Naive price-time priority matcher backed by MySQL rows.
 */
@Service
@RequiredArgsConstructor
public class TradeServiceImpl implements TradeService {

    private final TradeOrderMapper tradeOrderMapper;

    private final TradeExecutionMapper tradeExecutionMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TradeOrder place(Long userId, OrderCreateRequest request) {
        if (!OrderType.LIMIT.name().equalsIgnoreCase(request.getOrderType())) {
            throw new BizException(ResultCode.BAD_REQUEST, "only LIMIT orders supported in demo engine");
        }
        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "limit price required");
        }
        TradeOrder incoming = new TradeOrder();
        incoming.setUserId(userId);
        incoming.setSymbol(request.getSymbol());
        incoming.setSide(request.getSide().toUpperCase());
        incoming.setOrderType(request.getOrderType().toUpperCase());
        incoming.setPrice(request.getPrice());
        incoming.setQuantity(request.getQuantity());
        incoming.setFilledQuantity(BigDecimal.ZERO);
        incoming.setStatus(OrderStatus.OPEN.name());
        tradeOrderMapper.insert(incoming);
        match(incoming);
        return tradeOrderMapper.selectById(incoming.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancel(Long userId, Long orderId) {
        TradeOrder order = tradeOrderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        if (!OrderStatus.OPEN.name().equals(order.getStatus())
                && !OrderStatus.PARTIALLY_FILLED.name().equals(order.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "order not cancellable");
        }
        order.setStatus(OrderStatus.CANCELED.name());
        tradeOrderMapper.updateById(order);
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

    private void match(TradeOrder taker) {
        BigDecimal remaining = taker.getQuantity().subtract(taker.getFilledQuantity());
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        boolean buy = OrderSide.BUY.name().equalsIgnoreCase(taker.getSide());
        var q = Wrappers.<TradeOrder>lambdaQuery()
                .eq(TradeOrder::getSymbol, taker.getSymbol())
                .in(TradeOrder::getStatus, OrderStatus.OPEN.name(), OrderStatus.PARTIALLY_FILLED.name())
                .ne(TradeOrder::getUserId, taker.getUserId());
        if (buy) {
            q.eq(TradeOrder::getSide, OrderSide.SELL.name())
                    .le(TradeOrder::getPrice, taker.getPrice())
                    .orderByAsc(TradeOrder::getPrice)
                    .orderByAsc(TradeOrder::getId);
        } else {
            q.eq(TradeOrder::getSide, OrderSide.BUY.name())
                    .ge(TradeOrder::getPrice, taker.getPrice())
                    .orderByDesc(TradeOrder::getPrice)
                    .orderByAsc(TradeOrder::getId);
        }
        List<TradeOrder> makers = tradeOrderMapper.selectList(q);
        for (TradeOrder maker : makers) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal makerRem = maker.getQuantity().subtract(maker.getFilledQuantity());
            if (makerRem.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal qty = remaining.min(makerRem);
            BigDecimal price = maker.getPrice();
            TradeExecution exec = new TradeExecution();
            exec.setSymbol(taker.getSymbol());
            exec.setPrice(price);
            exec.setQuantity(qty);
            if (buy) {
                exec.setBuyOrderId(taker.getId());
                exec.setSellOrderId(maker.getId());
                exec.setBuyerId(taker.getUserId());
                exec.setSellerId(maker.getUserId());
            } else {
                exec.setBuyOrderId(maker.getId());
                exec.setSellOrderId(taker.getId());
                exec.setBuyerId(maker.getUserId());
                exec.setSellerId(taker.getUserId());
            }
            tradeExecutionMapper.insert(exec);
            maker.setFilledQuantity(maker.getFilledQuantity().add(qty));
            maker.setStatus(maker.getFilledQuantity().compareTo(maker.getQuantity()) >= 0
                    ? OrderStatus.FILLED.name()
                    : OrderStatus.PARTIALLY_FILLED.name());
            tradeOrderMapper.updateById(maker);
            taker.setFilledQuantity(taker.getFilledQuantity().add(qty));
            remaining = taker.getQuantity().subtract(taker.getFilledQuantity());
            taker.setStatus(taker.getFilledQuantity().compareTo(taker.getQuantity()) >= 0
                    ? OrderStatus.FILLED.name()
                    : OrderStatus.PARTIALLY_FILLED.name());
            tradeOrderMapper.updateById(taker);
        }
    }
}
