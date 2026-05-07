package io.github.wahhh.bacp.custody;

import io.github.wahhh.bacp.config.properties.BacpCustodyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lazily builds {@link Web3j} HTTP clients per configured chain profile.
 */
@Component
@RequiredArgsConstructor
public class BlockchainRpcRegistry {

    private final BacpCustodyProperties custodyProperties;

    private final Map<String, Web3j> cache = new ConcurrentHashMap<>();

    /**
     * Returns cached {@link Web3j} client for chain profile key ({@code ethereum}, {@code bsc}, {@code polygon}).
     *
     * @param profile chain profile key
     * @return web3j client
     */
    public Web3j web3j(String profile) {
        return cache.computeIfAbsent(profile.toLowerCase(), key -> {
            BacpCustodyProperties.ChainRpc rpc = custodyProperties.getChains().get(key);
            if (rpc == null || rpc.getRpcUrl() == null || rpc.getRpcUrl().isBlank()) {
                throw new IllegalStateException("RPC URL missing for chain profile: " + key);
            }
            return Web3j.build(new HttpService(rpc.getRpcUrl()));
        });
    }
}
