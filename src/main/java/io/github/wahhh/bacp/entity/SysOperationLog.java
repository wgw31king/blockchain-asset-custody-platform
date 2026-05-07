package io.github.wahhh.bacp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Audit trail row persisted by {@link io.github.wahhh.bacp.common.audit.OperationLogAspect}.
 */
@Data
@TableName("t_sys_operation_log")
public class SysOperationLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    private String username;

    private String module;

    private String action;

    private String params;

    private String ip;

    private Integer success;

    @TableField("error_msg")
    private String errorMsg;

    @TableField("duration_ms")
    private Long durationMs;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
