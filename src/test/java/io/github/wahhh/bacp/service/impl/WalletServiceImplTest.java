package io.github.wahhh.bacp.service.impl;

import io.github.wahhh.bacp.config.properties.BacpCryptoProperties;
import io.github.wahhh.bacp.entity.Wallet;
import io.github.wahhh.bacp.mapper.WalletMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Test
    void provisionsNewWallet() {
        when(walletMapper.selectOne(any())).thenReturn(null);
        when(walletMapper.insert(any(Wallet.class))).thenReturn(1);

        Wallet out = walletService.ensureWallet(2L, "BSC");
        assertNull(out.getEncryptedPrivateKey());
        verify(walletMapper).insert(any(Wallet.class));
    }
}
