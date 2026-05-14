package io.github.wahhh.bacp.monitor.job;

import io.github.wahhh.bacp.custody.BlockchainRpcRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.web3j.protocol.core.DefaultBlockParameterName;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Polls JSON-RPC {@code eth_blockNumber} for configured custody chains and exports head height gauges.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChainLagMetricsJob {

    private static final List<String> CHAINS = List.of("ethereum", "bsc", "polygon");

    private final BlockchainRpcRegistry blockchainRpcRegistry;

    private final MeterRegistry meterRegistry;

    private final Map<String, AtomicLong> headByChain = new ConcurrentHashMap<>();

    /**
     * Registers strong-reference gauges once at startup.
     */
    @PostConstruct
    void registerGauges() {
        for (String chain : CHAINS) {
            AtomicLong holder = new AtomicLong(0L);
            headByChain.put(chain, holder);
            Gauge.builder("bacp_chain_head_block", holder, AtomicLong::get)
                    .tag("chain", chain)
                    .description("Latest block height observed via custodial RPC profile")
                    .register(meterRegistry);
        }
    }

    /**
     * Polls RPC endpoints on a fixed delay.
     */
    @Scheduled(fixedDelayString = "${bacp.metrics.chain-poll-ms:30000}")
    public void pollLatestBlocks() {
        for (String chain : CHAINS) {
            AtomicLong holder = headByChain.get(chain);
            if (holder == null) {
                continue;
            }
            long startNs = System.nanoTime();
            try {
                var ethBlock = blockchainRpcRegistry.web3j(chain)
                        .ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false)
                        .send();
                if (ethBlock.getBlock() != null && ethBlock.getBlock().getNumber() != null) {
                    holder.set(ethBlock.getBlock().getNumber().longValue());
                }
            } catch (Exception ex) {
                log.debug("Skip chain head poll chain={} reason={}", chain, ex.getMessage());
            } finally {
                // Prometheus: bacp_chain_rpc_call_seconds_* — custodial RPC latency (not on-chain confirm time).
                meterRegistry.timer("bacp_chain_rpc_call_seconds", "chain", chain, "method", "eth_blockNumber")
                        .record(Duration.ofNanos(System.nanoTime() - startNs));
            }
        }
    }
}
