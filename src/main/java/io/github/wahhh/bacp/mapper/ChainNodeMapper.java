package io.github.wahhh.bacp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.wahhh.bacp.entity.ChainNode;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@link ChainNode} persistence.
 */
@Mapper
public interface ChainNodeMapper extends BaseMapper<ChainNode> {
}
