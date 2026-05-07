package io.github.wahhh.bacp.custody;

import io.github.wahhh.bacp.config.properties.BacpCustodyProperties;
import org.junit.jupiter.api.Test;
import org.web3j.protocol.Web3j;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BlockchainRpcRegistryTest {

    @Test
    void cachesClientPerProfile() {
        BacpCustodyProperties props = new BacpCustodyProperties();
        BacpCustodyProperties.ChainRpc eth = new BacpCustodyProperties.ChainRpc();
        eth.setRpcUrl("http://127.0.0.1:8545");
        props.getChains().put("ethereum", eth);

        BlockchainRpcRegistry registry = new BlockchainRpcRegistry(props);
        Web3j a = registry.web3j("ethereum");
        Web3j b = registry.web3j("ETHEREUM");
        assertSame(a, b);
    }

    @Test
    void missingRpcFailsFast() {
        BacpCustodyProperties props = new BacpCustodyProperties();
        BlockchainRpcRegistry registry = new BlockchainRpcRegistry(props);
        assertThrows(IllegalStateException.class, () -> registry.web3j("unknown"));
    }
}
