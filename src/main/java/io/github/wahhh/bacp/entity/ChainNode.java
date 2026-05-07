package io.github.wahhh.bacp.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Blockchain RPC endpoint configuration.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_chain_node")
public class ChainNode extends BaseEntity {

    @TableField("chain_type")
    private String chainType;

    @TableField("node_name")
    private String nodeName;

    @TableField("rpc_url")
    private String rpcUrl;

    @TableField("ws_url")
    private String wsUrl;

    private Integer priority;

    private Integer enabled;
}
