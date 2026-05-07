package io.github.wahhh.bacp.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Account balance snapshot per currency with optimistic locking.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_balance")
public class Balance extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    @TableField("currency_id")
    private Long currencyId;

    @TableField("available_amount")
    private BigDecimal availableAmount;

    @TableField("frozen_amount")
    private BigDecimal frozenAmount;

    @Version
    private Integer version;
}
