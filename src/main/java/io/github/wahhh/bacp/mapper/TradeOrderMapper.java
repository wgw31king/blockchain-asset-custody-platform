package io.github.wahhh.bacp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.wahhh.bacp.entity.TradeOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

/**
 * {@link TradeOrder} persistence.
 */
@Mapper
public interface TradeOrderMapper extends BaseMapper<TradeOrder> {

    /**
     * Best ask (lowest resting sell limit) for market buy sizing / freeze estimation.
     *
     * @param symbol trading pair
     * @return min limit price among open sells or null when book empty
     */
    @Select("""
            SELECT MIN(price) FROM t_order
            WHERE deleted = 0 AND symbol = #{symbol} AND side = 'SELL' AND order_type = 'LIMIT'
              AND status IN ('PENDING', 'PARTIALLY_FILLED')
              AND price IS NOT NULL AND quantity > filled_quantity
            """)
    BigDecimal selectBestAsk(@Param("symbol") String symbol);

    /**
     * Best bid (highest resting buy limit) for market sell freeze estimation.
     *
     * @param symbol trading pair
     * @return max limit price among open buys or null when book empty
     */
    @Select("""
            SELECT MAX(price) FROM t_order
            WHERE deleted = 0 AND symbol = #{symbol} AND side = 'BUY' AND order_type = 'LIMIT'
              AND status IN ('PENDING', 'PARTIALLY_FILLED')
              AND price IS NOT NULL AND quantity > filled_quantity
            """)
    BigDecimal selectBestBid(@Param("symbol") String symbol);
}
