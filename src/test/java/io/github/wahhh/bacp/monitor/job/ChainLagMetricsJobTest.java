package io.github.wahhh.bacp.monitor.job;

import io.github.wahhh.bacp.custody.BlockchainRpcRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChainLagMetricsJobTest {

    @Test
    void pollSwallowsRpcFailures() {
        BlockchainRpcRegistry registry = mock(BlockchainRpcRegistry.class);
        when(registry.web3j(anyString())).thenThrow(new IllegalStateException("no rpc"));
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ChainLagMetricsJob job = new ChainLagMetricsJob(registry, meterRegistry);
        job.registerGauges();
        job.pollLatestBlocks();
    }
}
