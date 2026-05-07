package io.github.wahhh.bacp.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Tradable / custody asset metadata.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_currency")
public class Currency extends BaseEntity {

    private String symbol;

    private String name;

    @TableField("asset_type")
    private String assetType;

    @TableField("chain_type")
    private String chainType;

    @TableField("contract_address")
    private String contractAddress;

    private Integer decimals;

    private Integer enabled;
}
