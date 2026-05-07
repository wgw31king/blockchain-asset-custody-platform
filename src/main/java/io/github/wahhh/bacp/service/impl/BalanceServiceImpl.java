package io.github.wahhh.bacp.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.wahhh.bacp.common.exception.BizException;
import io.github.wahhh.bacp.common.result.ResultCode;
import io.github.wahhh.bacp.entity.Balance;
import io.github.wahhh.bacp.mapper.BalanceMapper;
import io.github.wahhh.bacp.service.BalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Default balance ledger backed by {@link BalanceMapper}.
 */
@Service
@RequiredArgsConstructor
public class BalanceServiceImpl implements BalanceService {

    private final BalanceMapper balanceMapper;

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
        Balance bal = balanceMapper.selectOne(Wrappers.<Balance>lambdaQuery()
                .eq(Balance::getUserId, userId)
                .eq(Balance::getCurrencyId, currencyId));
        if (bal == null) {
            bal = new Balance();
            bal.setUserId(userId);
            bal.setCurrencyId(currencyId);
            bal.setAvailableAmount(amount);
            bal.setFrozenAmount(BigDecimal.ZERO);
            balanceMapper.insert(bal);
            return;
        }
        bal.setAvailableAmount(bal.getAvailableAmount().add(amount));
        int rows = balanceMapper.updateById(bal);
        if (rows == 0) {
            throw new BizException(ResultCode.CONFLICT, "optimistic lock conflict on credit");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void debit(Long userId, Long currencyId, BigDecimal amount) {
        Balance bal = balanceMapper.selectOne(Wrappers.<Balance>lambdaQuery()
                .eq(Balance::getUserId, userId)
                .eq(Balance::getCurrencyId, currencyId));
        if (bal == null || bal.getAvailableAmount().compareTo(amount) < 0) {
            throw new BizException(ResultCode.INSUFFICIENT_BALANCE);
        }
        bal.setAvailableAmount(bal.getAvailableAmount().subtract(amount));
        int rows = balanceMapper.updateById(bal);
        if (rows == 0) {
            throw new BizException(ResultCode.CONFLICT, "optimistic lock conflict on debit");
        }
    }
}
