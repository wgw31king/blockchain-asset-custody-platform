package io.github.wahhh.bacp.service.impl;

import io.github.wahhh.bacp.common.constant.MQConstants;
import io.github.wahhh.bacp.common.util.JsonUtil;
import io.github.wahhh.bacp.common.enums.TxDirection;
import io.github.wahhh.bacp.common.enums.TxStatus;
import io.github.wahhh.bacp.dto.request.WithdrawRequest;
import io.github.wahhh.bacp.entity.TxRecord;
import io.github.wahhh.bacp.mapper.TxRecordMapper;
import io.github.wahhh.bacp.service.BalanceService;
import io.github.wahhh.bacp.service.WithdrawService;
import io.github.wahhh.bacp.service.risk.RiskEngine;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Queues withdrawal processing after balance debit.
 */
@Service
@RequiredArgsConstructor
public class WithdrawServiceImpl implements WithdrawService {

    private final BalanceService balanceService;

    private final TxRecordMapper txRecordMapper;

    private final RabbitTemplate rabbitTemplate;

    private final RiskEngine riskEngine;

    private final MeterRegistry meterRegistry;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submit(Long userId, WithdrawRequest request) {
        riskEngine.validateWithdraw(userId, request.getAmount());
        balanceService.debit(userId, request.getCurrencyId(), request.getAmount());
        TxRecord tx = new TxRecord();
        tx.setUserId(userId);
        tx.setCurrencyId(request.getCurrencyId());
        tx.setChainType(request.getChainType().toLowerCase());
        tx.setDirection(TxDirection.WITHDRAW.name());
        tx.setToAddress(request.getToAddress());
        tx.setAmount(request.getAmount());
        tx.setStatus(TxStatus.PENDING.name());
        txRecordMapper.insert(tx);
        rabbitTemplate.convertAndSend(MQConstants.EXCHANGE_TX, MQConstants.ROUTING_WITHDRAW,
                JsonUtil.toJson(Map.of("txId", tx.getId())));
        meterRegistry.counter("bacp_tx_total",
                "direction", "WITHDRAW",
                "chain", request.getChainType(),
                "status", TxStatus.PENDING.name()).increment();
        return tx.getId();
    }
}
