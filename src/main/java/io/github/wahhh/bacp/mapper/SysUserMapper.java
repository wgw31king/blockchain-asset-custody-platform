package io.github.wahhh.bacp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.wahhh.bacp.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@link SysUser} persistence.
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
