package io.github.wahhh.bacp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.wahhh.bacp.entity.Balance;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@link Balance} persistence.
 */
@Mapper
public interface BalanceMapper extends BaseMapper<Balance> {
}
