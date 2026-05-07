package io.github.wahhh.bacp.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Spot order resting or matched in the demo matching engine.
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

    private String status;
}
