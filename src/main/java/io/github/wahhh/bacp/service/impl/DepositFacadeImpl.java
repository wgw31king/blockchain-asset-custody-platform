package io.github.wahhh.bacp.service.impl;

import io.github.wahhh.bacp.common.constant.CacheKeys;
import io.github.wahhh.bacp.common.enums.TxDirection;
import io.github.wahhh.bacp.common.enums.TxStatus;
import io.github.wahhh.bacp.common.exception.BizException;
import io.github.wahhh.bacp.common.result.ResultCode;
import io.github.wahhh.bacp.config.properties.BacpCustodyProperties;
import io.github.wahhh.bacp.dto.request.DepositNotifyRequest;
import io.github.wahhh.bacp.entity.TxRecord;
import io.github.wahhh.bacp.mapper.TxRecordMapper;
import io.github.wahhh.bacp.service.BalanceService;
import io.github.wahhh.bacp.service.DepositFacade;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * Core deposit pipeline writing {@link TxRecord} + balance credits.
 */
@Service
@RequiredArgsConstructor
public class DepositFacadeImpl implements DepositFacade {

    private final TxRecordMapper txRecordMapper;

    private final BalanceService balanceService;

    private final StringRedisTemplate stringRedisTemplate;

    private final BacpCustodyProperties custodyProperties;

    private final MeterRegistry meterRegistry;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleDeposit(String idempotencyKey, DepositNotifyRequest body) {
        String keyBase = idempotencyKey != null && !idempotencyKey.isBlank()
                ? idempotencyKey
                : body.getTxHash();
        String redisKey = CacheKeys.IDEMPOTENCY + keyBase;
        Boolean fresh = stringRedisTemplate.opsForValue().setIfAbsent(redisKey, "1", Duration.ofHours(24));
        if (Boolean.FALSE.equals(fresh)) {
            throw new BizException(ResultCode.DUPLICATE_REQUEST);
        }
        int confirmations = body.getConfirmations() == null ? 0 : body.getConfirmations();
        TxStatus status = confirmations >= custodyProperties.getDefaultConfirmations()
                ? TxStatus.CONFIRMED
                : TxStatus.PENDING;
        TxRecord tx = new TxRecord();
        tx.setUserId(body.getUserId());
        tx.setCurrencyId(body.getCurrencyId());
        tx.setChainType(body.getChainType().toLowerCase());
        tx.setDirection(TxDirection.DEPOSIT.name());
        tx.setTxHash(body.getTxHash());
        tx.setFromAddress(body.getFromAddress());
        tx.setToAddress(body.getToAddress());
        tx.setAmount(body.getAmount());
        tx.setFee(java.math.BigDecimal.ZERO);
        tx.setStatus(status.name());
        tx.setConfirmations(confirmations);
        try {
            txRecordMapper.insert(tx);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ResultCode.DUPLICATE_REQUEST);
        }
        if (status == TxStatus.CONFIRMED) {
            balanceService.credit(body.getUserId(), body.getCurrencyId(), body.getAmount());
        }
        meterRegistry.counter("bacp_tx_total",
                "direction", "DEPOSIT",
                "chain", body.getChainType(),
                "status", status.name()).increment();
    }
}
