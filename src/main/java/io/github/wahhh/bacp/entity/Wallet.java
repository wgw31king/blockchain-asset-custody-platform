package io.github.wahhh.bacp.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * User-owned on-chain address record with encrypted signing material.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_wallet")
public class Wallet extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    @TableField("chain_type")
    private String chainType;

    private String address;

    @TableField("encrypted_private_key")
    private String encryptedPrivateKey;

    @TableField("derivation_path")
    private String derivationPath;
}
