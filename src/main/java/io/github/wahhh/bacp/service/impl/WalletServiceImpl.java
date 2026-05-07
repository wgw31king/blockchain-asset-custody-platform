package io.github.wahhh.bacp.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.wahhh.bacp.common.util.AesUtil;
import io.github.wahhh.bacp.config.properties.BacpCryptoProperties;
import io.github.wahhh.bacp.entity.Wallet;
import io.github.wahhh.bacp.mapper.WalletMapper;
import io.github.wahhh.bacp.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

/**
 * Demo wallet provisioning using locally generated EC keys encrypted at rest.
 */
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletMapper walletMapper;

    private final BacpCryptoProperties cryptoProperties;

    /**
     * {@inheritDoc}
     */
    @Override
    public Wallet ensureWallet(Long userId, String chainProfile) {
        Wallet existing = walletMapper.selectOne(Wrappers.<Wallet>lambdaQuery()
                .eq(Wallet::getUserId, userId)
                .eq(Wallet::getChainType, chainProfile.toLowerCase()));
        if (existing != null) {
            existing.setEncryptedPrivateKey(null);
            return existing;
        }
        try {
            ECKeyPair kp = Keys.createEcKeyPair();
            String pkHex = Numeric.toHexStringNoPrefixZeroPadded(kp.getPrivateKey(), Keys.PRIVATE_KEY_LENGTH_IN_HEX);
            String enc = AesUtil.encrypt(pkHex, cryptoProperties.getMasterKey());
            Wallet w = new Wallet();
            w.setUserId(userId);
            w.setChainType(chainProfile.toLowerCase());
            w.setAddress("0x" + Keys.getAddress(kp));
            w.setEncryptedPrivateKey(enc);
            w.setDerivationPath("m/44'/60'/0'/0/" + userId);
            walletMapper.insert(w);
            w.setEncryptedPrivateKey(null);
            return w;
        } catch (Exception ex) {
            throw new IllegalStateException("wallet generation failed", ex);
        }
    }
}
