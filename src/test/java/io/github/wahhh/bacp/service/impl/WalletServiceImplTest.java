package io.github.wahhh.bacp.service.impl;

import io.github.wahhh.bacp.config.properties.BacpCryptoProperties;
import io.github.wahhh.bacp.entity.Wallet;
import io.github.wahhh.bacp.mapper.WalletMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private WalletMapper walletMapper;

    private final BacpCryptoProperties crypto = new BacpCryptoProperties();

    private WalletServiceImpl walletService;

    @BeforeEach
    void init() {
        crypto.setMasterKey("unit-test-master-key-32bytes-minimum!!");
        walletService = new WalletServiceImpl(walletMapper, crypto);
    }

    /**
     * 场景：用户在该链已有钱包，直接返回地址且不暴露密文私钥字段。
     */
    @Test
    void returnsExistingWithoutPrivateMaterial() {
        Wallet w = new Wallet();
        w.setAddress("0xabc");
        w.setEncryptedPrivateKey("secret");
        when(walletMapper.selectOne(any())).thenReturn(w);

        Wallet out = walletService.ensureWallet(1L, "ethereum");
        assertEquals("0xabc", out.getAddress());
        assertNull(out.getEncryptedPrivateKey());
    }

    /**
     * 场景：首次创建钱包，生成 0x 地址、链类型小写、derivationPath 包含账户索引。
     */
    @Test
    void provisionsNewWalletWithNormalizedChainAndDerivationPath() {
        when(walletMapper.selectOne(any())).thenReturn(null);
        when(walletMapper.insert(any(Wallet.class))).thenReturn(1);

        Wallet out = walletService.ensureWallet(2L, "BSC");
        assertNull(out.getEncryptedPrivateKey());
        assertNotNull(out.getAddress());
        assertTrue(out.getAddress().startsWith("0x"));
        assertEquals("bsc", out.getChainType());
        assertEquals("m/44'/60'/0'/0/2", out.getDerivationPath());

        ArgumentCaptor<Wallet> cap = ArgumentCaptor.forClass(Wallet.class);
        verify(walletMapper).insert(cap.capture());
        assertEquals("bsc", cap.getValue().getChainType());
        assertNotNull(cap.getValue().getEncryptedPrivateKey());
    }

    /**
     * 场景：主密钥为空导致加密链路异常，应包装为 IllegalStateException。
     */
    @Test
    void wrapsEncryptionFailureWhenMasterKeyMissing() {
        crypto.setMasterKey(null);
        walletService = new WalletServiceImpl(walletMapper, crypto);
        when(walletMapper.selectOne(any())).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> walletService.ensureWallet(3L, "ethereum"));
    }
}
