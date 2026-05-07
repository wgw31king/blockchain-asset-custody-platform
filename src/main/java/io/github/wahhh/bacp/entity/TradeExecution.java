package io.github.wahhh.bacp.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Resulting trade fill between two orders.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_trade")
public class TradeExecution extends BaseEntity {

    private String symbol;

    @TableField("buy_order_id")
    private Long buyOrderId;

    @TableField("sell_order_id")
    private Long sellOrderId;

    private BigDecimal price;

    private BigDecimal quantity;

    @TableField("buyer_id")
    private Long buyerId;

    @TableField("seller_id")
    private Long sellerId;
}
