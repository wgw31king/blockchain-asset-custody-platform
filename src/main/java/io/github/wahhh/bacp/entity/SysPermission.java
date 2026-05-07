package io.github.wahhh.bacp.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Fine-grained permission (typically button-level).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_sys_permission")
public class SysPermission extends BaseEntity {

    @TableField("perm_code")
    private String permCode;

    @TableField("perm_name")
    private String permName;

    @TableField("parent_id")
    private Long parentId;

    @TableField("perm_type")
    private String permType;
}
