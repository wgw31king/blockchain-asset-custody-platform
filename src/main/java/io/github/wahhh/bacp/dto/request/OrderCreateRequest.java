package io.github.wahhh.bacp.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Spot order placement payload for the production matching engine.
 */
@Data
@Schema(description = "Create spot order")
public class OrderCreateRequest {

    @NotBlank
    @Schema(description = "Trading pair", example = "ETH-USDT", requiredMode = Schema.RequiredMode.REQUIRED)
    private String symbol;

    @NotBlank
    @Pattern(regexp = "BUY|SELL|buy|sell", message = "side must be BUY or SELL")
    @Schema(description = "Order side", example = "BUY", allowableValues = {"BUY", "SELL", "buy", "sell"})
    private String side;

    @NotBlank
    @Pattern(regexp = "LIMIT|MARKET|limit|market", message = "orderType must be LIMIT or MARKET")
    @Schema(description = "LIMIT requires positive `price`", example = "LIMIT")
    private String orderType;

    @Schema(description = "Limit price (required for LIMIT orders)", example = "2000.50")
    private BigDecimal price;

    @NotNull
    @DecimalMin(value = "0", inclusive = false, message = "quantity must be greater than zero")
    @Schema(description = "Order quantity in base asset", example = "0.01", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal quantity;
}
