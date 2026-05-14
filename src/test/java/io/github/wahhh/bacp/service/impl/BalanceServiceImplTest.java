package io.github.wahhh.bacp.service.impl;

import io.github.wahhh.bacp.common.exception.BizException;
import io.github.wahhh.bacp.common.result.ResultCode;
import io.github.wahhh.bacp.entity.Balance;
import io.github.wahhh.bacp.mapper.BalanceMapper;
import io.github.wahhh.bacp.mapper.CapitalFlowMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BalanceServiceImplTest {

    @Mock
    private BalanceMapper balanceMapper;

    @Mock
    private CapitalFlowMapper capitalFlowMapper;

    @InjectMocks
    private BalanceServiceImpl balanceService;

    @Test
    void creditCreatesRowWhenMissing() {
        when(balanceMapper.selectOne(any())).thenReturn(null);
        when(balanceMapper.insert(any(Balance.class))).thenAnswer(inv -> {
            Balance b = inv.getArgument(0);
            b.setId(42L);
            return 1;
        });
        when(balanceMapper.updateById(any(Balance.class))).thenReturn(1);

        balanceService.credit(1L, 2L, new BigDecimal("3"));

        verify(balanceMapper).insert(any(Balance.class));
        verify(balanceMapper).updateById(any(Balance.class));
    }

    @Test
    void debitRejectsMissingBalance() {
        when(balanceMapper.selectOne(any())).thenReturn(null);
        when(balanceMapper.insert(any(Balance.class))).thenAnswer(inv -> {
            Balance b = inv.getArgument(0);
            b.setId(7L);
            return 1;
        });
        BizException ex = assertThrows(BizException.class,
                () -> balanceService.debit(1L, 2L, BigDecimal.ONE));
        assertEquals(ResultCode.INSUFFICIENT_BALANCE.getCode(), ex.getCode());
    }
}
