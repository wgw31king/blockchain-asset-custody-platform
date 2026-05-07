package io.github.wahhh.bacp.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.wahhh.bacp.common.constant.MQConstants;
import io.github.wahhh.bacp.common.enums.TxStatus;
import io.github.wahhh.bacp.common.util.JsonUtil;
import io.github.wahhh.bacp.entity.TxRecord;
import io.github.wahhh.bacp.mapper.TxRecordMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Simulates on-chain broadcast + confirmation for withdrawals.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawMessageListener {

    private final TxRecordMapper txRecordMapper;

    private final MeterRegistry meterRegistry;

    /**
     * Consumes withdrawal jobs from RabbitMQ.
     *
     * @param payload message body containing txId
     */
    @RabbitListener(queues = MQConstants.QUEUE_WITHDRAW)
    public void onMessage(String json) {
        Map<String, Object> payload = JsonUtil.fromJson(json, new TypeReference<Map<String, Object>>() {
        });
        Object raw = payload.get("txId");
        if (raw == null) {
            return;
        }
        long txId = Long.parseLong(raw.toString());
        TxRecord tx = txRecordMapper.selectById(txId);
        if (tx == null) {
            return;
        }
        tx.setTxHash("0x" + UUID.randomUUID().toString().replace("-", ""));
        tx.setStatus(TxStatus.CONFIRMED.name());
        tx.setConfirmations(32);
        txRecordMapper.updateById(tx);
        meterRegistry.counter("bacp_tx_total",
                "direction", "WITHDRAW",
                "chain", tx.getChainType(),
                "status", TxStatus.CONFIRMED.name()).increment();
        log.info("Withdraw tx {} confirmed (demo)", txId);
    }
}
