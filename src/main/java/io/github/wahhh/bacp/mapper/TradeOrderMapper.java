package io.github.wahhh.bacp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.wahhh.bacp.entity.TradeOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@link TradeOrder} persistence.
 */
@Mapper
public interface TradeOrderMapper extends BaseMapper<TradeOrder> {
}
