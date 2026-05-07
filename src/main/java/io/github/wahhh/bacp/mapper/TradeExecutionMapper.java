package io.github.wahhh.bacp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.wahhh.bacp.entity.TradeExecution;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@link TradeExecution} persistence.
 */
@Mapper
public interface TradeExecutionMapper extends BaseMapper<TradeExecution> {
}
