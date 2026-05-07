package io.github.wahhh.bacp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.wahhh.bacp.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * {@link SysRole} persistence.
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * Loads role codes granted to a user.
     *
     * @param userId user id
     * @return role codes
     */
    @Select("""
            SELECT r.role_code
            FROM t_sys_role r
            JOIN t_sys_user_role ur ON r.id = ur.role_id AND ur.deleted = 0
            WHERE ur.user_id = #{userId}
              AND r.deleted = 0
            """)
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);
}
