package io.github.wahhh.bacp.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * User withdrawal intent.
 */
@Data
@Schema(description = "Withdraw request")
public class WithdrawRequest {

    @NotNull
    @Schema(description = "Currency row id", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long currencyId;

    @NotBlank
    @Schema(description = "Destination address", example = "0xRecipient...")
    private String toAddress;

    @NotNull
    @DecimalMin("0")
    @Schema(description = "Amount to send", example = "0.01")
    private BigDecimal amount;

    @NotBlank
    @Schema(description = "Chain profile", example = "ethereum", allowableValues = {"ethereum", "bsc", "polygon"})
    private String chainType;
}
