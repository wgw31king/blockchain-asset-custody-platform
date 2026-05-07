package io.github.wahhh.bacp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.wahhh.bacp.entity.Wallet;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@link Wallet} persistence.
 */
@Mapper
public interface WalletMapper extends BaseMapper<Wallet> {
}
