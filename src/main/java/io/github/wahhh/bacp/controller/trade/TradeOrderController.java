package io.github.wahhh.bacp.controller.trade;

import io.github.wahhh.bacp.common.result.Result;
import io.github.wahhh.bacp.common.web.SecurityHelper;
import io.github.wahhh.bacp.dto.request.OrderCreateRequest;
import io.github.wahhh.bacp.entity.TradeExecution;
import io.github.wahhh.bacp.entity.TradeOrder;
import io.github.wahhh.bacp.service.TradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Demo spot trading endpoints.
 */
@Tag(name = "Trade")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TradeOrderController {

    private final TradeService tradeService;

    /**
     * Places a limit order and triggers matching.
     *
     * @param body order payload
     * @return persisted order
     */
    @Operation(summary = "Create order")
    @PostMapping("/orders")
    @PreAuthorize("hasAuthority('trade:order')")
    public Result<TradeOrder> create(@Valid @RequestBody OrderCreateRequest body) {
        Long uid = SecurityHelper.currentUserIdOrThrow();
        return Result.ok(tradeService.place(uid, body));
    }

    /**
     * Cancels open order owned by caller.
     *
     * @param id order id
     * @return empty payload
     */
    @Operation(summary = "Cancel order")
    @DeleteMapping("/orders/{id}")
    @PreAuthorize("hasAuthority('trade:order')")
    public Result<Void> cancel(@PathVariable Long id) {
        Long uid = SecurityHelper.currentUserIdOrThrow();
        tradeService.cancel(uid, id);
        return Result.ok();
    }

    /**
     * Lists orders with optional filters.
     *
     * @param symbol optional symbol filter
     * @return orders
     */
    @Operation(summary = "List orders")
    @GetMapping("/orders")
    @PreAuthorize("hasAuthority('trade:order')")
    public Result<List<TradeOrder>> orders(@RequestParam(required = false) String symbol) {
        Long uid = SecurityHelper.currentUserIdOrThrow();
        return Result.ok(tradeService.listOrders(uid, symbol));
    }

    /**
     * Lists recent executions.
     *
     * @param symbol optional symbol filter
     * @return trades
     */
    @Operation(summary = "List trades")
    @GetMapping("/trades")
    @PreAuthorize("hasAuthority('trade:order')")
    public Result<List<TradeExecution>> trades(@RequestParam(required = false) String symbol) {
        return Result.ok(tradeService.listTrades(symbol));
    }
}
