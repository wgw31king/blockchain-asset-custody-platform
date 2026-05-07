package io.github.wahhh.bacp.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Many-to-many link between roles and permissions.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_sys_role_permission")
public class SysRolePermission extends BaseEntity {

    @TableField("role_id")
    private Long roleId;

    @TableField("perm_id")
    private Long permId;
}
