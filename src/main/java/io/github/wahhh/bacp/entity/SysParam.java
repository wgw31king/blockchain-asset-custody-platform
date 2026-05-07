package io.github.wahhh.bacp.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Key/value system configuration row.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_sys_param")
public class SysParam extends BaseEntity {

    @TableField("param_key")
    private String paramKey;

    @TableField("param_value")
    private String paramValue;

    private String remark;
}
