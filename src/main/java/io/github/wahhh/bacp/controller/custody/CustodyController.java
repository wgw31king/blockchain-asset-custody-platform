package io.github.wahhh.bacp.controller.custody;

import io.github.wahhh.bacp.common.result.Result;
import io.github.wahhh.bacp.common.web.SecurityHelper;
import io.github.wahhh.bacp.config.openapi.OpenApiExamples;
import io.github.wahhh.bacp.dto.request.DepositNotifyRequest;
import io.github.wahhh.bacp.dto.request.WithdrawRequest;
import io.github.wahhh.bacp.entity.Balance;
import io.github.wahhh.bacp.entity.Wallet;
import io.github.wahhh.bacp.service.BalanceService;
import io.github.wahhh.bacp.service.DepositFacade;
import io.github.wahhh.bacp.service.WalletService;
import io.github.wahhh.bacp.service.WithdrawService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Custody REST endpoints (wallet / balances / deposits / withdrawals).
 */
@Tag(
        name = "Custody",
        description = "Balances, wallets, withdrawals for authenticated users; deposit notify for trusted indexers "
                + "(`custody:deposit:notify`).")
@RestController
@RequestMapping("/api/v1/custody")
@RequiredArgsConstructor
public class CustodyController {

    private final DepositFacade depositFacade;

    private final WithdrawService withdrawService;

    private final BalanceService balanceService;

    private final WalletService walletService;

    @Operation(
            summary = "Notify confirmed deposit",
            description =
                    "Idempotent deposit notification from an indexer. Requires authority `custody:deposit:notify`. "
                            + "Duplicates may return `code` 2002.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Accepted",
                    content =
                            @Content(
                                    mediaType = "application/json",
                                    examples =
                                            @ExampleObject(name = "Ok", value = OpenApiExamples.RES_OK_VOID))),
            @ApiResponse(
                    responseCode = "401",
                    description = "Missing JWT",
                    content =
                            @Content(
                                    mediaType = "application/json",
                                    examples =
                                            @ExampleObject(
                                                    name = "Unauthorized",
                                                    value = OpenApiExamples.RES_UNAUTHORIZED))),
            @ApiResponse(
                    responseCode = "403",
                    description = "Insufficient authority or signing/IP rules",
                    content =
                            @Content(
                                    mediaType = "application/json",
                                    examples =
                                            @ExampleObject(
                                                    name = "Forbidden",
                                                    value = OpenApiExamples.RES_FORBIDDEN)))
    })
    @PostMapping("/deposits/notify")
    @PreAuthorize("hasAuthority('custody:deposit:notify')")
    public Result<Void> notifyDeposit(
            @Parameter(description = "Optional idempotency key for safe retries")
                    @RequestHeader(value = "Idempotency-Key", required = false)
                    String idempotencyKey,
            @Valid @RequestBody DepositNotifyRequest body) {
        depositFacade.handleDeposit(idempotencyKey, body);
        return Result.ok();
    }

    @Operation(
            summary = "Submit withdrawal",
            description =
                    "Creates withdrawal intent after balance and risk checks. Requires `custody:withdraw`. May return "
                            + "risk or balance related business codes (HTTP 200 body).")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Withdraw record id in `data`",
                    content = @Content(mediaType = "application/json")),
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
    @PostMapping("/withdraw")
    @PreAuthorize("hasAuthority('custody:withdraw')")
    public Result<Long> withdraw(@Valid @RequestBody WithdrawRequest body) {
        Long uid = SecurityHelper.currentUserIdOrThrow();
        return Result.ok(withdrawService.submit(uid, body));
    }

    @Operation(summary = "List balances", description = "All balance rows for the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Balance list", content = @Content(mediaType = "application/json")),
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
    @GetMapping("/balances")
    public Result<List<Balance>> balances() {
        Long uid = SecurityHelper.currentUserIdOrThrow();
        return Result.ok(balanceService.list(uid));
    }

    @Operation(
            summary = "Ensure wallet address",
            description =
                    "Creates custodial wallet for `chainProfile` if missing (`ethereum`, `bsc`, `polygon`). "
                            + "Does not return private key material.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Wallet row", content = @Content(mediaType = "application/json")),
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
    @PostMapping("/wallets/{chainProfile}/ensure")
    public Result<Wallet> ensureWallet(
            @Parameter(description = "Chain profile key", example = "ethereum") @PathVariable String chainProfile) {
        Long uid = SecurityHelper.currentUserIdOrThrow();
        return Result.ok(walletService.ensureWallet(uid, chainProfile));
    }
}
