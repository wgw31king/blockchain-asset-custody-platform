package io.github.wahhh.bacp.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Spot order resting or matched in the production matching engine.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_order")
public class TradeOrder extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    private String symbol;

    private String side;

    @TableField("order_type")
    private String orderType;

    private BigDecimal price;

    private BigDecimal quantity;

    @TableField("filled_quantity")
    private BigDecimal filledQuantity;

    @TableField("frozen_quote_amount")
    private BigDecimal frozenQuoteAmount;

    @TableField("frozen_base_amount")
    private BigDecimal frozenBaseAmount;

    private String status;

    @TableField("expires_at")
    private LocalDateTime expiresAt;
}
