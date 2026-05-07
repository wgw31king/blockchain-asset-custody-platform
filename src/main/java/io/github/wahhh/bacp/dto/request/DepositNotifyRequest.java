package io.github.wahhh.bacp.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * External indexer deposit notification payload.
 */
@Data
@Schema(description = "Deposit notify payload")
public class DepositNotifyRequest {

    @NotNull
    @Schema(description = "Custody user id")
    private Long userId;

    @NotNull
    @Schema(description = "Currency row id")
    private Long currencyId;

    @NotBlank
    @Schema(description = "ethereum | bsc | polygon")
    private String chainType;

    @NotBlank
    @Schema(description = "On-chain transaction hash")
    private String txHash;

    private String fromAddress;

    private String toAddress;

    @NotNull
    @DecimalMin("0")
    @Schema(description = "Amount credited")
    private BigDecimal amount;

    @Schema(description = "Confirmations observed")
    private Integer confirmations;
}
