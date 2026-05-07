package io.github.wahhh.bacp.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Blockchain ledger movement row (deposit / withdraw / transfer).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_tx")
public class TxRecord extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    @TableField("currency_id")
    private Long currencyId;

    @TableField("chain_type")
    private String chainType;

    private String direction;

    @TableField("tx_hash")
    private String txHash;

    @TableField("from_address")
    private String fromAddress;

    @TableField("to_address")
    private String toAddress;

    private BigDecimal amount;

    private BigDecimal fee;

    private String status;

    private Integer confirmations;

    @TableField("block_number")
    private Long blockNumber;

    private String remark;
}
