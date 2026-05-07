package io.github.wahhh.bacp.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Persisted risk engine alert payload.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_risk_alert")
public class RiskAlert extends BaseEntity {

    @TableField("rule_code")
    private String ruleCode;

    @TableField("user_id")
    private Long userId;

    @TableField("payload_json")
    private String payloadJson;

    private String status;
}
