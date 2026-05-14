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
    @Schema(description = "Custody user id", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;

    @NotNull
    @Schema(description = "Currency row id (`t_currency.id`)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long currencyId;

    @NotBlank
    @Schema(description = "Chain profile", example = "ethereum", allowableValues = {"ethereum", "bsc", "polygon"})
    private String chainType;

    @NotBlank
    @Schema(description = "On-chain transaction hash", example = "0xabc...")
    private String txHash;

    @Schema(description = "Source address (optional)", example = "0xFrom...")
    private String fromAddress;

    @Schema(description = "Destination deposit address (optional)", example = "0xTo...")
    private String toAddress;

    @NotNull
    @DecimalMin("0")
    @Schema(description = "Amount credited (decimal)", example = "0.5")
    private BigDecimal amount;

    @Schema(description = "Confirmations observed", example = "12")
    private Integer confirmations;
}
