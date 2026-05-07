package io.github.wahhh.bacp.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Platform operator / end-user account.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_sys_user")
public class SysUser extends BaseEntity {

    private String username;

    @TableField("password_hash")
    private String passwordHash;

    private String email;

    private String nickname;

    /** 1 active, 0 disabled. */
    private Integer status;

    @TableField("last_login_at")
    private LocalDateTime lastLoginAt;
}
