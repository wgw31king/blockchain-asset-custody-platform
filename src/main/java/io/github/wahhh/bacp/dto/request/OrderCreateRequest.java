package io.github.wahhh.bacp.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Spot order placement payload for demo matching engine.
 */
@Data
@Schema(description = "Create spot order")
public class OrderCreateRequest {

    @NotBlank
    @Schema(example = "ETH-USDT")
    private String symbol;

    @NotBlank
    @Schema(example = "BUY")
    private String side;

    @NotBlank
    @Schema(example = "LIMIT")
    private String orderType;

    private BigDecimal price;

    @NotNull
    @DecimalMin("0")
    private BigDecimal quantity;
}
