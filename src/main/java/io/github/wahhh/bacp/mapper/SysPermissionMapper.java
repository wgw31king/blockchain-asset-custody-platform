package io.github.wahhh.bacp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.wahhh.bacp.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * {@link SysPermission} persistence.
 */
@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    /**
     * Loads distinct permission codes granted to a user via roles.
     *
     * @param userId user id
     * @return permission codes
     */
    @Select("""
            SELECT DISTINCT p.perm_code
            FROM t_sys_permission p
            JOIN t_sys_role_permission rp ON p.id = rp.perm_id AND rp.deleted = 0
            JOIN t_sys_user_role ur ON rp.role_id = ur.role_id AND ur.deleted = 0
            WHERE ur.user_id = #{userId}
              AND p.deleted = 0
            """)
    List<String> selectPermCodesByUserId(@Param("userId") Long userId);
}
