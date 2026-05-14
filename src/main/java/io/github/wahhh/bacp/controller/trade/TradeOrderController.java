package io.github.wahhh.bacp.controller.trade;

import io.github.wahhh.bacp.common.audit.OperationLog;
import io.github.wahhh.bacp.common.result.Result;
import io.github.wahhh.bacp.common.web.SecurityHelper;
import io.github.wahhh.bacp.config.openapi.OpenApiExamples;
import io.github.wahhh.bacp.dto.request.OrderCreateRequest;
import io.github.wahhh.bacp.entity.TradeExecution;
import io.github.wahhh.bacp.entity.TradeOrder;
import io.github.wahhh.bacp.service.TradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@Tag(
        name = "Trade",
        description = "Demo matcher — place/cancel/list orders and list trades. Requires `trade:order`.")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TradeOrderController {

    private final TradeService tradeService;

    @Operation(
            summary = "Create order",
            description =
                    "Places LIMIT or MARKET order and runs matching. Validation and liquidity errors use HTTP 200 "
                            + "with non-success `code`. Audited (`order:create`).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Persisted order in `data`", content = @Content(mediaType = "application/json")),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bean validation failure",
                    content =
                            @Content(
                                    mediaType = "application/json",
                                    examples =
                                            @ExampleObject(
                                                    name = "Validation",
                                                    value = OpenApiExamples.RES_VALIDATION))),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated",
                    content =
                            @Content(
                                    mediaType = "application/json",
                                    examples =
                                            @ExampleObject(
                                                    name = "Unauthorized",
                                                    value = OpenApiExamples.RES_UNAUTHORIZED)))
    })
    @OperationLog(module = "trade", action = "order:create", recordParams = true)
    @PostMapping("/orders")
    @PreAuthorize("hasAuthority('trade:order')")
    public Result<TradeOrder> create(@Valid @RequestBody OrderCreateRequest body) {
        Long uid = SecurityHelper.currentUserIdOrThrow();
        return Result.ok(tradeService.place(uid, body));
    }

    @Operation(
            summary = "Cancel order",
            description = "Cancels open order owned by caller. Conflict when order is being matched.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cancelled", content = @Content(mediaType = "application/json")),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated",
                    content =
                            @Content(
                                    mediaType = "application/json",
                                    examples =
                                            @ExampleObject(
                                                    name = "Unauthorized",
                                                    value = OpenApiExamples.RES_UNAUTHORIZED)))
    })
    @OperationLog(module = "trade", action = "order:cancel", recordParams = true)
    @DeleteMapping("/orders/{id}")
    @PreAuthorize("hasAuthority('trade:order')")
    public Result<Void> cancel(@Parameter(description = "Order id") @PathVariable Long id) {
        Long uid = SecurityHelper.currentUserIdOrThrow();
        tradeService.cancel(uid, id);
        return Result.ok();
    }

    @Operation(summary = "List orders", description = "Caller-owned orders; optional `symbol` filter.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order list", content = @Content(mediaType = "application/json")),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated",
                    content =
                            @Content(
                                    mediaType = "application/json",
                                    examples =
                                            @ExampleObject(
                                                    name = "Unauthorized",
                                                    value = OpenApiExamples.RES_UNAUTHORIZED)))
    })
    @GetMapping("/orders")
    @PreAuthorize("hasAuthority('trade:order')")
    public Result<List<TradeOrder>> orders(
            @Parameter(description = "Trading pair symbol, e.g. ETH-USDT") @RequestParam(required = false) String symbol) {
        Long uid = SecurityHelper.currentUserIdOrThrow();
        return Result.ok(tradeService.listOrders(uid, symbol));
    }

    @Operation(summary = "List trades", description = "Recent executions; optional `symbol` filter.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trade fills", content = @Content(mediaType = "application/json")),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated",
                    content =
                            @Content(
                                    mediaType = "application/json",
                                    examples =
                                            @ExampleObject(
                                                    name = "Unauthorized",
                                                    value = OpenApiExamples.RES_UNAUTHORIZED)))
    })
    @GetMapping("/trades")
    @PreAuthorize("hasAuthority('trade:order')")
    public Result<List<TradeExecution>> trades(
            @Parameter(description = "Trading pair symbol filter") @RequestParam(required = false) String symbol) {
        return Result.ok(tradeService.listTrades(symbol));
    }
}
