package io.github.wahhh.bacp.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Immutable accounting ledger entry for balance mutations.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_capital_flow")
public class CapitalFlow extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    @TableField("currency_id")
    private Long currencyId;

    private String direction;

    private BigDecimal amount;

    @TableField("balance_before")
    private BigDecimal balanceBefore;

    @TableField("balance_after")
    private BigDecimal balanceAfter;

    @TableField("ref_type")
    private String refType;

    @TableField("ref_id")
    private Long refId;

    private String remark;
}
