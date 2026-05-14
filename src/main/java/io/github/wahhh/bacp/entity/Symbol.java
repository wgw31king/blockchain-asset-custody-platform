package io.github.wahhh.bacp.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Tradable pair metadata linking a logical symbol (e.g. ETH-USDT) to base/quote currency rows.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_symbol")
public class Symbol extends BaseEntity {

    private String symbol;

    @TableField("base_currency_id")
    private Long baseCurrencyId;

    @TableField("quote_currency_id")
    private Long quoteCurrencyId;

    @TableField("price_scale")
    private Integer priceScale;

    @TableField("qty_scale")
    private Integer qtyScale;

    @TableField("min_qty")
    private BigDecimal minQty;

    @TableField("min_notional")
    private BigDecimal minNotional;

    private Integer enabled;
}
