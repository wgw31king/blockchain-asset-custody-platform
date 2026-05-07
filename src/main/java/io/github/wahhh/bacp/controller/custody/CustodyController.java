package io.github.wahhh.bacp.controller.custody;

import io.github.wahhh.bacp.common.result.Result;
import io.github.wahhh.bacp.common.web.SecurityHelper;
import io.github.wahhh.bacp.dto.request.DepositNotifyRequest;
import io.github.wahhh.bacp.dto.request.WithdrawRequest;
import io.github.wahhh.bacp.entity.Balance;
import io.github.wahhh.bacp.entity.Wallet;
import io.github.wahhh.bacp.service.BalanceService;
import io.github.wahhh.bacp.service.DepositFacade;
import io.github.wahhh.bacp.service.WalletService;
import io.github.wahhh.bacp.service.WithdrawService;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "Custody")
@RestController
@RequestMapping("/api/v1/custody")
@RequiredArgsConstructor
public class CustodyController {

    private final DepositFacade depositFacade;

    private final WithdrawService withdrawService;

    private final BalanceService balanceService;

    private final WalletService walletService;

    /**
     * Accepts canonical deposit notifications from trusted indexers.
     *
     * @param idempotencyKey optional idempotency header
     * @param body           payload
     * @return empty body
     */
    @Operation(summary = "Notify confirmed deposit")
    @PostMapping("/deposits/notify")
    @PreAuthorize("hasAuthority('custody:deposit:notify')")
    public Result<Void> notifyDeposit(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody DepositNotifyRequest body) {
        depositFacade.handleDeposit(idempotencyKey, body);
        return Result.ok();
    }

    /**
     * Submits withdraw intent for async signing pipeline.
     *
     * @param body withdraw payload
     * @return tx id
     */
    @Operation(summary = "Submit withdrawal")
    @PostMapping("/withdraw")
    @PreAuthorize("hasAuthority('custody:withdraw')")
    public Result<Long> withdraw(@Valid @RequestBody WithdrawRequest body) {
        Long uid = SecurityHelper.currentUserIdOrThrow();
        return Result.ok(withdrawService.submit(uid, body));
    }

    /**
     * Lists balances for authenticated user.
     *
     * @return balances
     */
    @Operation(summary = "List balances")
    @GetMapping("/balances")
    public Result<List<Balance>> balances() {
        Long uid = SecurityHelper.currentUserIdOrThrow();
        return Result.ok(balanceService.list(uid));
    }

    /**
     * Ensures custodial wallet exists for chain profile.
     *
     * @param chainProfile ethereum | bsc | polygon
     * @return wallet row without private key
     */
    @Operation(summary = "Ensure wallet address")
    @PostMapping("/wallets/{chainProfile}/ensure")
    public Result<Wallet> ensureWallet(@PathVariable String chainProfile) {
        Long uid = SecurityHelper.currentUserIdOrThrow();
        return Result.ok(walletService.ensureWallet(uid, chainProfile));
    }
}
