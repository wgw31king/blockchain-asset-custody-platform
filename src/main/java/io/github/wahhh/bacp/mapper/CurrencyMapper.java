package io.github.wahhh.bacp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.wahhh.bacp.entity.Currency;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@link Currency} persistence.
 */
@Mapper
public interface CurrencyMapper extends BaseMapper<Currency> {
}
