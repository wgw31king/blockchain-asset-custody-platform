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
    @Schema(description = "Currency row id")
    private Long currencyId;

    @NotBlank
    @Schema(description = "Destination address")
    private String toAddress;

    @NotNull
    @DecimalMin("0")
    @Schema(description = "Amount to send")
    private BigDecimal amount;

    @NotBlank
    @Schema(description = "ethereum | bsc | polygon")
    private String chainType;
}
