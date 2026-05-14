package io.github.wahhh.bacp.service.trade;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.wahhh.bacp.common.enums.OrderSide;
import io.github.wahhh.bacp.common.enums.OrderStatus;
import io.github.wahhh.bacp.common.enums.OrderType;
import io.github.wahhh.bacp.common.exception.BizException;
import io.github.wahhh.bacp.common.result.ResultCode;
import io.github.wahhh.bacp.config.properties.BacpTradeProperties;
import io.github.wahhh.bacp.dto.request.OrderCreateRequest;
import io.github.wahhh.bacp.entity.Symbol;
import io.github.wahhh.bacp.entity.TradeExecution;
import io.github.wahhh.bacp.entity.TradeOrder;
import io.github.wahhh.bacp.mapper.TradeExecutionMapper;
import io.github.wahhh.bacp.mapper.TradeOrderMapper;
import io.github.wahhh.bacp.service.BalanceService;
import io.github.wahhh.bacp.service.SymbolService;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DB-transactional core of the matcher: freeze funds, persist orders/fills, settle balances, and
 * enforce the {@link OrderStatusTransitions} graph. Invoked only while the caller holds the
 * per-symbol Redisson lock from {@link MatchingEngine}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingTransactionService {

    private static final String REF_ORDER = "ORDER";

    private static final String REF_TRADE = "TRADE";

    private final SymbolService symbolService;

    private final BalanceService balanceService;

    private final TradeOrderMapper tradeOrderMapper;

    private final TradeExecutionMapper tradeExecutionMapper;

    private final BacpTradeProperties tradeProperties;

    private final OrderAuditWriter orderAuditWriter;

    private final MeterRegistry meterRegistry;

    /**
     * Validates the request, reserves balances, inserts the taker row, matches against resting
     * liquidity, and finalises status within a single database transaction (rolls back on any
     * failure — the intended “order rollback” mechanism).
     *
     * @param userId  taker user id
     * @param request placement payload
     * @return latest persisted taker snapshot
     */
    @Transactional(rollbackFor = Exception.class)
    public TradeOrder placeAndMatch(Long userId, OrderCreateRequest request) {
        Symbol sym = symbolService.requireEnabled(request.getSymbol());
        String symbol = sym.getSymbol();
        OrderSide side = parseSide(request.getSide());
        OrderType orderType = parseOrderType(request.getOrderType());
        BigDecimal qty = nz(request.getQuantity()).stripTrailingZeros();
        validateQuantity(sym, qty);
        BigDecimal price = request.getPrice() == null ? null : nz(request.getPrice()).stripTrailingZeros();
        if (orderType == OrderType.LIMIT) {
            if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException(ResultCode.VALIDATION_ERROR, "limit price required and must be positive");
            }
        }
        BigDecimal notional = price == null ? BigDecimal.ZERO : price.multiply(qty);
        if (orderType == OrderType.LIMIT && notional.compareTo(nz(sym.getMinNotional())) < 0) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "order below min notional");
        }

        BigDecimal freezeQuote = BigDecimal.ZERO;
        BigDecimal freezeBase = BigDecimal.ZERO;
        if (side == OrderSide.BUY) {
            if (orderType == OrderType.LIMIT) {
                freezeQuote = scaleMoney(price.multiply(qty), sym.getPriceScale() + sym.getQtyScale());
            } else {
                BigDecimal bestAsk = tradeOrderMapper.selectBestAsk(symbol);
                if (bestAsk == null) {
                    throw new BizException(ResultCode.BIZ_ERROR, "no liquidity for market buy");
                }
                freezeQuote = scaleMoney(bestAsk.multiply(qty), sym.getPriceScale() + sym.getQtyScale());
            }
        } else {
            freezeBase = scaleQty(qty, sym.getQtyScale());
            if (orderType == OrderType.MARKET) {
                BigDecimal bestBid = tradeOrderMapper.selectBestBid(symbol);
                if (bestBid == null) {
                    throw new BizException(ResultCode.BIZ_ERROR, "no liquidity for market sell");
                }
            }
        }

        TradeOrder taker = new TradeOrder();
        taker.setUserId(userId);
        taker.setSymbol(symbol);
        taker.setSide(side.name());
        taker.setOrderType(orderType.name());
        taker.setPrice(price);
        taker.setQuantity(qty);
        taker.setFilledQuantity(BigDecimal.ZERO);
        taker.setFrozenQuoteAmount(freezeQuote);
        taker.setFrozenBaseAmount(freezeBase);
        taker.setStatus(OrderStatus.PENDING.name());
        taker.setExpiresAt(LocalDateTime.now().plusSeconds(tradeProperties.getOrderTtlSeconds()));
        tradeOrderMapper.insert(taker);
        // Prometheus: bacp_trade_order_created_total{symbol,side,order_type}
        meterRegistry.counter("bacp_trade_order_created_total",
                "symbol", symbol,
                "side", side.name(),
                "order_type", orderType.name()).increment();

        if (side == OrderSide.BUY) {
            balanceService.freeze(userId, sym.getQuoteCurrencyId(), freezeQuote, REF_ORDER, taker.getId());
        } else {
            balanceService.freeze(userId, sym.getBaseCurrencyId(), freezeBase, REF_ORDER, taker.getId());
        }

        advance(taker, OrderStatus.PENDING, OrderStatus.PROCESSING, userId, Map.of("phase", "match_start"));

        runMatchLoop(sym, taker);

        TradeOrder out = tradeOrderMapper.selectById(taker.getId());
        meterRegistry.counter("bacp_trade_order_total", "result", out.getStatus()).increment();
        return out;
    }

    /**
     * User-initiated cancel: releases remaining reservation and marks the order cancelled.
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelByUser(Long userId, Long orderId) {
        TradeOrder order = requireOrder(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        cancelInternal(order, userId, false);
    }

    /**
     * Scheduler-driven expiry cancel (skips ownership check after TTL validation).
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelExpired(Long orderId) {
        TradeOrder order = requireOrder(orderId);
        if (order.getExpiresAt() == null) {
            return;
        }
        if (!order.getExpiresAt().isBefore(LocalDateTime.now())) {
            return;
        }
        OrderStatus st = OrderStatusTransitions.parse(order.getStatus());
        if (st != OrderStatus.PENDING && st != OrderStatus.PARTIALLY_FILLED) {
            return;
        }
        cancelInternal(order, null, true);
        meterRegistry.counter("bacp_trade_timeout_total").increment();
    }

    private void cancelInternal(TradeOrder order, Long userIdForAudit, boolean expired) {
        OrderStatus cur = OrderStatusTransitions.parse(order.getStatus());
        if (cur == OrderStatus.FILLED || cur == OrderStatus.CANCELLED || cur == OrderStatus.REJECTED) {
            throw new BizException(ResultCode.BAD_REQUEST, "order not cancellable");
        }
        if (cur == OrderStatus.PROCESSING) {
            throw new BizException(ResultCode.CONFLICT, "order is being matched");
        }
        Symbol sym = symbolService.requireEnabled(order.getSymbol());
        releaseAllFrozen(order, sym);
        advance(order, cur, OrderStatus.CANCELLED, userIdForAudit,
                Map.of("reason", expired ? "expired" : "user_cancel"));
        // Prometheus: bacp_trade_order_cancelled_total{reason=USER_CANCEL|TIMEOUT_SCHEDULER}
        meterRegistry.counter("bacp_trade_order_cancelled_total", "reason",
                expired ? "TIMEOUT_SCHEDULER" : "USER_CANCEL").increment();
    }

    private TradeOrder requireOrder(Long orderId) {
        TradeOrder order = tradeOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return order;
    }

    private void runMatchLoop(Symbol sym, TradeOrder taker) {
        int iterations = 0;
        while (iterations++ < tradeProperties.getMaxMatchIterations()) {
            TradeOrder current = tradeOrderMapper.selectById(taker.getId());
            BigDecimal remaining = current.getQuantity().subtract(nz(current.getFilledQuantity()));
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            List<TradeOrder> makers = selectMakers(current);
            if (makers.isEmpty()) {
                break;
            }
            boolean progressed = false;
            for (TradeOrder maker : makers) {
                TradeOrder m = tradeOrderMapper.selectById(maker.getId());
                if (m == null) {
                    continue;
                }
                BigDecimal mRem = m.getQuantity().subtract(nz(m.getFilledQuantity()));
                if (mRem.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                TradeOrder t = tradeOrderMapper.selectById(taker.getId());
                BigDecimal tRem = t.getQuantity().subtract(nz(t.getFilledQuantity()));
                if (tRem.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
                BigDecimal fillQty = tRem.min(mRem);
                BigDecimal tradePrice = m.getPrice();
                applyFill(sym, t, m, fillQty, tradePrice);
                progressed = true;
            }
            if (!progressed) {
                break;
            }
        }
        finalizeTaker(sym, tradeOrderMapper.selectById(taker.getId()));
    }

    private List<TradeOrder> selectMakers(TradeOrder taker) {
        boolean buy = OrderSide.BUY.name().equalsIgnoreCase(taker.getSide());
        OrderType takerType = OrderType.valueOf(taker.getOrderType().trim().toUpperCase());
        var q = Wrappers.<TradeOrder>lambdaQuery()
                .eq(TradeOrder::getSymbol, taker.getSymbol())
                .in(TradeOrder::getStatus, OrderStatus.PENDING.name(), OrderStatus.PARTIALLY_FILLED.name())
                .eq(TradeOrder::getOrderType, OrderType.LIMIT.name())
                .ne(TradeOrder::getUserId, taker.getUserId())
                .ne(TradeOrder::getId, taker.getId());
        if (buy) {
            q.eq(TradeOrder::getSide, OrderSide.SELL.name());
            if (takerType == OrderType.LIMIT && taker.getPrice() != null) {
                q.le(TradeOrder::getPrice, taker.getPrice());
            }
            q.orderByAsc(TradeOrder::getPrice).orderByAsc(TradeOrder::getId);
        } else {
            q.eq(TradeOrder::getSide, OrderSide.BUY.name());
            if (takerType == OrderType.LIMIT && taker.getPrice() != null) {
                q.ge(TradeOrder::getPrice, taker.getPrice());
            }
            q.orderByDesc(TradeOrder::getPrice).orderByAsc(TradeOrder::getId);
        }
        return tradeOrderMapper.selectList(q);
    }

    private void applyFill(Symbol sym, TradeOrder taker, TradeOrder maker, BigDecimal qty, BigDecimal price) {
        BigDecimal notional = scaleMoney(price.multiply(qty), sym.getPriceScale() + sym.getQtyScale());
        BigDecimal feeRate = tradeProperties.getFeeRate() == null ? BigDecimal.ZERO : tradeProperties.getFeeRate();
        BigDecimal feeFactor = BigDecimal.ONE.subtract(feeRate).max(BigDecimal.ZERO);
        BigDecimal baseOut = scaleQty(qty, sym.getQtyScale());
        BigDecimal baseIn = scaleQty(qty.multiply(feeFactor), sym.getQtyScale());
        BigDecimal quoteIn = scaleMoney(notional.multiply(feeFactor), sym.getPriceScale() + sym.getQtyScale());

        Long buyerId;
        Long sellerId;
        Long buyOrderId;
        Long sellOrderId;
        if (OrderSide.BUY.name().equalsIgnoreCase(taker.getSide())) {
            buyerId = taker.getUserId();
            sellerId = maker.getUserId();
            buyOrderId = taker.getId();
            sellOrderId = maker.getId();
        } else {
            buyerId = maker.getUserId();
            sellerId = taker.getUserId();
            buyOrderId = maker.getId();
            sellOrderId = taker.getId();
        }

        TradeExecution exec = new TradeExecution();
        exec.setSymbol(sym.getSymbol());
        exec.setBuyOrderId(buyOrderId);
        exec.setSellOrderId(sellOrderId);
        exec.setPrice(price);
        exec.setQuantity(qty);
        exec.setBuyerId(buyerId);
        exec.setSellerId(sellerId);
        tradeExecutionMapper.insert(exec);
        Long tradeId = exec.getId();

        balanceService.settleSpendFrozen(buyerId, sym.getQuoteCurrencyId(), notional, REF_TRADE, tradeId);
        balanceService.settleReceiveAvailable(buyerId, sym.getBaseCurrencyId(), baseIn, REF_TRADE, tradeId);
        balanceService.settleSpendFrozen(sellerId, sym.getBaseCurrencyId(), baseOut, REF_TRADE, tradeId);
        balanceService.settleReceiveAvailable(sellerId, sym.getQuoteCurrencyId(), quoteIn, REF_TRADE, tradeId);

        bumpFill(taker, qty, sym, notional, true);
        bumpFill(maker, qty, sym, notional, false);

        meterRegistry.counter("bacp_trade_match_total", "symbol", sym.getSymbol()).increment();
        // Prometheus: bacp_trade_notional_quote_sum / _count (DistributionSummary; double approximation)
        DistributionSummary.builder("bacp_trade_notional_quote")
                .description("Matched notional in quote currency units per fill")
                .tag("symbol", sym.getSymbol())
                .register(meterRegistry)
                .record(notional.doubleValue());
    }

    private void bumpFill(TradeOrder row, BigDecimal qty, Symbol sym, BigDecimal notional, boolean isTaker) {
        OrderStatus before = OrderStatusTransitions.parse(row.getStatus());
        BigDecimal newFilled = nz(row.getFilledQuantity()).add(qty);
        row.setFilledQuantity(newFilled);
        if (OrderSide.BUY.name().equalsIgnoreCase(row.getSide())) {
            row.setFrozenQuoteAmount(nz(row.getFrozenQuoteAmount()).subtract(notional).max(BigDecimal.ZERO));
        } else {
            row.setFrozenBaseAmount(nz(row.getFrozenBaseAmount()).subtract(qty).max(BigDecimal.ZERO));
        }
        boolean full = newFilled.compareTo(row.getQuantity()) >= 0;
        OrderStatus next;
        if (full) {
            next = OrderStatus.FILLED;
        } else {
            next = OrderStatus.PARTIALLY_FILLED;
        }
        OrderStatusTransitions.assertCanTransition(before, next);
        row.setStatus(next.name());
        tradeOrderMapper.updateById(row);
        orderAuditWriter.writeOrderStatus(row.getUserId(), row.getId(), before, next,
                Map.of("fillQty", qty.toPlainString(), "taker", isTaker));
    }

    private void finalizeTaker(Symbol sym, TradeOrder taker) {
        TradeOrder row = taker;
        OrderStatus cur = OrderStatusTransitions.parse(row.getStatus());
        BigDecimal remaining = row.getQuantity().subtract(nz(row.getFilledQuantity()));
        OrderType ot = OrderType.valueOf(row.getOrderType().trim().toUpperCase());

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            reconcileSurplusReservation(sym, row, remaining);
        }

        row = tradeOrderMapper.selectById(row.getId());
        cur = OrderStatusTransitions.parse(row.getStatus());
        remaining = row.getQuantity().subtract(nz(row.getFilledQuantity()));

        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            if (cur != OrderStatus.FILLED) {
                advance(row, cur, OrderStatus.FILLED, row.getUserId(), Map.of("phase", "done"));
            }
            releaseAllFrozen(tradeOrderMapper.selectById(row.getId()), sym);
            return;
        }

        if (ot == OrderType.MARKET) {
            releaseAllFrozen(row, sym);
            row = tradeOrderMapper.selectById(row.getId());
            cur = OrderStatusTransitions.parse(row.getStatus());
            if (nz(row.getFilledQuantity()).compareTo(BigDecimal.ZERO) > 0) {
                if (cur != OrderStatus.PARTIALLY_FILLED) {
                    advance(row, cur, OrderStatus.PARTIALLY_FILLED, row.getUserId(), Map.of("phase", "market_partial"));
                }
            } else {
                if (cur != OrderStatus.REJECTED) {
                    advance(row, cur, OrderStatus.REJECTED, row.getUserId(), Map.of("phase", "market_no_fill"));
                }
            }
            return;
        }

        // LIMIT resting
        row = tradeOrderMapper.selectById(row.getId());
        cur = OrderStatusTransitions.parse(row.getStatus());
        if (nz(row.getFilledQuantity()).compareTo(BigDecimal.ZERO) == 0) {
            if (cur != OrderStatus.PENDING) {
                advance(row, cur, OrderStatus.PENDING, row.getUserId(), Map.of("phase", "resting"));
            }
        } else {
            if (cur != OrderStatus.PARTIALLY_FILLED) {
                advance(row, cur, OrderStatus.PARTIALLY_FILLED, row.getUserId(), Map.of("phase", "resting_partial"));
            }
        }
    }

    private void reconcileSurplusReservation(Symbol sym, TradeOrder taker, BigDecimal remaining) {
        if (OrderSide.BUY.name().equalsIgnoreCase(taker.getSide()) && taker.getPrice() != null) {
            BigDecimal must = scaleMoney(taker.getPrice().multiply(remaining), sym.getPriceScale() + sym.getQtyScale());
            BigDecimal onRow = nz(taker.getFrozenQuoteAmount());
            if (onRow.compareTo(must) > 0) {
                BigDecimal drop = onRow.subtract(must);
                balanceService.unfreeze(taker.getUserId(), sym.getQuoteCurrencyId(), drop, REF_ORDER, taker.getId());
                taker.setFrozenQuoteAmount(must);
                tradeOrderMapper.updateById(taker);
            }
        } else if (OrderSide.SELL.name().equalsIgnoreCase(taker.getSide())) {
            BigDecimal must = scaleQty(remaining, sym.getQtyScale());
            BigDecimal onRow = nz(taker.getFrozenBaseAmount());
            if (onRow.compareTo(must) > 0) {
                BigDecimal drop = onRow.subtract(must);
                balanceService.unfreeze(taker.getUserId(), sym.getBaseCurrencyId(), drop, REF_ORDER, taker.getId());
                taker.setFrozenBaseAmount(must);
                tradeOrderMapper.updateById(taker);
            }
        }
    }

    private void releaseAllFrozen(TradeOrder order, Symbol sym) {
        if (nz(order.getFrozenQuoteAmount()).compareTo(BigDecimal.ZERO) > 0) {
            balanceService.unfreeze(order.getUserId(), sym.getQuoteCurrencyId(), order.getFrozenQuoteAmount(), REF_ORDER, order.getId());
            order.setFrozenQuoteAmount(BigDecimal.ZERO);
        }
        if (nz(order.getFrozenBaseAmount()).compareTo(BigDecimal.ZERO) > 0) {
            balanceService.unfreeze(order.getUserId(), sym.getBaseCurrencyId(), order.getFrozenBaseAmount(), REF_ORDER, order.getId());
            order.setFrozenBaseAmount(BigDecimal.ZERO);
        }
        tradeOrderMapper.updateById(order);
    }

    private void advance(TradeOrder row, OrderStatus from, OrderStatus to, Long auditUserId, Map<String, Object> extra) {
        OrderStatus parsed = OrderStatusTransitions.parse(row.getStatus());
        OrderStatus effectiveFrom = from;
        if (parsed != effectiveFrom) {
            if (parsed == to) {
                return;
            }
            effectiveFrom = parsed;
        }
        OrderStatusTransitions.assertCanTransition(effectiveFrom, to);
        row.setStatus(to.name());
        tradeOrderMapper.updateById(row);
        Map<String, Object> payload = new HashMap<>(extra);
        payload.put("symbol", row.getSymbol());
        orderAuditWriter.writeOrderStatus(auditUserId, row.getId(), effectiveFrom, to, payload);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static void validateQuantity(Symbol sym, BigDecimal qty) {
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "quantity must be positive");
        }
        if (sym.getMinQty() != null && sym.getMinQty().compareTo(BigDecimal.ZERO) > 0
                && qty.compareTo(sym.getMinQty()) < 0) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "quantity below symbol min");
        }
    }

    private static BigDecimal scaleMoney(BigDecimal v, int scale) {
        return v.setScale(Math.min(18, Math.max(0, scale)), RoundingMode.HALF_UP);
    }

    private static BigDecimal scaleQty(BigDecimal v, int scale) {
        return v.setScale(Math.min(18, Math.max(0, scale)), RoundingMode.HALF_UP);
    }

    private static OrderSide parseSide(String side) {
        try {
            return OrderSide.valueOf(side.trim().toUpperCase());
        } catch (Exception ex) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "invalid side");
        }
    }

    private static OrderType parseOrderType(String t) {
        try {
            return OrderType.valueOf(t.trim().toUpperCase());
        } catch (Exception ex) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "invalid order type");
        }
    }
}
