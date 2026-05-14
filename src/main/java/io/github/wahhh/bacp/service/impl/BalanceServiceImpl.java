package io.github.wahhh.bacp.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.wahhh.bacp.common.constant.TradeLedgerDirection;
import io.github.wahhh.bacp.common.exception.BizException;
import io.github.wahhh.bacp.common.result.ResultCode;
import io.github.wahhh.bacp.entity.Balance;
import io.github.wahhh.bacp.entity.CapitalFlow;
import io.github.wahhh.bacp.mapper.BalanceMapper;
import io.github.wahhh.bacp.mapper.CapitalFlowMapper;
import io.github.wahhh.bacp.service.BalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Default balance ledger backed by {@link BalanceMapper} with trade freeze/settlement flows audited
 * to {@link CapitalFlow}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceServiceImpl implements BalanceService {

    private static final int OPTIMISTIC_RETRIES = 5;

    private static final String REF_ORDER = "ORDER";

    private final BalanceMapper balanceMapper;

    private final CapitalFlowMapper capitalFlowMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Balance> list(Long userId) {
        return balanceMapper.selectList(Wrappers.<Balance>lambdaQuery().eq(Balance::getUserId, userId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void credit(Long userId, Long currencyId, BigDecimal amount) {
        mutate(userId, currencyId, bal -> {
            BigDecimal before = bal.getAvailableAmount();
            bal.setAvailableAmount(before.add(amount));
            recordFlow(userId, currencyId, "CREDIT", amount, before, bal.getAvailableAmount(), "SYSTEM", null, "credit");
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void debit(Long userId, Long currencyId, BigDecimal amount) {
        mutate(userId, currencyId, bal -> {
            if (bal.getAvailableAmount().compareTo(amount) < 0) {
                throw new BizException(ResultCode.INSUFFICIENT_BALANCE);
            }
            BigDecimal before = bal.getAvailableAmount();
            bal.setAvailableAmount(before.subtract(amount));
            recordFlow(userId, currencyId, "DEBIT", amount, before, bal.getAvailableAmount(), "SYSTEM", null, "debit");
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void freeze(Long userId, Long currencyId, BigDecimal amount, String refType, Long refId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "freeze amount must be positive");
        }
        mutate(userId, currencyId, bal -> {
            if (bal.getAvailableAmount().compareTo(amount) < 0) {
                throw new BizException(ResultCode.INSUFFICIENT_BALANCE);
            }
            BigDecimal avBefore = bal.getAvailableAmount();
            BigDecimal frBefore = bal.getFrozenAmount();
            bal.setAvailableAmount(avBefore.subtract(amount));
            bal.setFrozenAmount(frBefore.add(amount));
            recordFlow(userId, currencyId, TradeLedgerDirection.FREEZE, amount, avBefore, bal.getAvailableAmount(),
                    refType, refId, "available->frozen");
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfreeze(Long userId, Long currencyId, BigDecimal amount, String refType, Long refId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        mutate(userId, currencyId, bal -> {
            if (bal.getFrozenAmount().compareTo(amount) < 0) {
                throw new BizException(ResultCode.BIZ_ERROR, "unfreeze exceeds frozen balance");
            }
            BigDecimal avBefore = bal.getAvailableAmount();
            BigDecimal frBefore = bal.getFrozenAmount();
            bal.setFrozenAmount(frBefore.subtract(amount));
            bal.setAvailableAmount(avBefore.add(amount));
            recordFlow(userId, currencyId, TradeLedgerDirection.UNFREEZE, amount, avBefore, bal.getAvailableAmount(),
                    refType, refId, "frozen->available");
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settleSpendFrozen(Long userId, Long currencyId, BigDecimal amount, String refType, Long refId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "settle spend must be positive");
        }
        mutate(userId, currencyId, bal -> {
            if (bal.getFrozenAmount().compareTo(amount) < 0) {
                throw new BizException(ResultCode.BIZ_ERROR, "settle spend exceeds frozen");
            }
            BigDecimal avBefore = bal.getAvailableAmount();
            bal.setFrozenAmount(bal.getFrozenAmount().subtract(amount));
            // Spend removes frozen; notional leaves the system into the counterparty leg
            recordFlow(userId, currencyId, TradeLedgerDirection.SETTLE_SPEND, amount, avBefore, bal.getAvailableAmount(),
                    refType, refId, "frozen consumed by match");
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settleReceiveAvailable(Long userId, Long currencyId, BigDecimal amount, String refType, Long refId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "settle receive must be positive");
        }
        mutate(userId, currencyId, bal -> {
            BigDecimal before = bal.getAvailableAmount();
            bal.setAvailableAmount(before.add(amount));
            recordFlow(userId, currencyId, TradeLedgerDirection.SETTLE_RECEIVE, amount, before, bal.getAvailableAmount(),
                    refType, refId, "match credit available");
        });
    }

    /**
     * Applies a mutation with optimistic retries to survive concurrent custody updates on the same row.
     */
    private void mutate(Long userId, Long currencyId, java.util.function.Consumer<Balance> op) {
        for (int attempt = 1; attempt <= OPTIMISTIC_RETRIES; attempt++) {
            Balance bal = balanceMapper.selectOne(Wrappers.<Balance>lambdaQuery()
                    .eq(Balance::getUserId, userId)
                    .eq(Balance::getCurrencyId, currencyId));
            if (bal == null) {
                Balance init = new Balance();
                init.setUserId(userId);
                init.setCurrencyId(currencyId);
                init.setAvailableAmount(BigDecimal.ZERO);
                init.setFrozenAmount(BigDecimal.ZERO);
                balanceMapper.insert(init);
                Balance loaded = balanceMapper.selectOne(Wrappers.<Balance>lambdaQuery()
                        .eq(Balance::getUserId, userId)
                        .eq(Balance::getCurrencyId, currencyId));
                // Unit tests may stub only insert; production MyBatis returns the row — fall back to init with generated id.
                bal = loaded != null ? loaded : init;
            }
            op.accept(bal);
            int rows = balanceMapper.updateById(bal);
            if (rows == 1) {
                return;
            }
            log.debug("Optimistic balance conflict user={} currency={} attempt={}", userId, currencyId, attempt);
        }
        throw new BizException(ResultCode.CONFLICT, "optimistic lock conflict on balance");
    }

    private void recordFlow(Long userId, Long currencyId, String direction, BigDecimal amount,
            BigDecimal balanceBefore, BigDecimal balanceAfter, String refType, Long refId, String remark) {
        CapitalFlow cf = new CapitalFlow();
        cf.setUserId(userId);
        cf.setCurrencyId(currencyId);
        cf.setDirection(direction);
        cf.setAmount(amount);
        cf.setBalanceBefore(balanceBefore);
        cf.setBalanceAfter(balanceAfter);
        cf.setRefType(refType);
        cf.setRefId(refId);
        cf.setRemark(remark);
        capitalFlowMapper.insert(cf);
    }
}
