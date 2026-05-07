package io.github.wahhh.bacp.service.impl;

import io.github.wahhh.bacp.common.exception.BizException;
import io.github.wahhh.bacp.common.result.ResultCode;
import io.github.wahhh.bacp.entity.Balance;
import io.github.wahhh.bacp.mapper.BalanceMapper;
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

    @InjectMocks
    private BalanceServiceImpl balanceService;

    @Test
    void creditCreatesRowWhenMissing() {
        when(balanceMapper.selectOne(any())).thenReturn(null);
        when(balanceMapper.insert(any(Balance.class))).thenReturn(1);

        balanceService.credit(1L, 2L, new BigDecimal("3"));

        verify(balanceMapper).insert(any(Balance.class));
    }

    @Test
    void debitRejectsMissingBalance() {
        when(balanceMapper.selectOne(any())).thenReturn(null);
        BizException ex = assertThrows(BizException.class,
                () -> balanceService.debit(1L, 2L, BigDecimal.ONE));
        assertEquals(ResultCode.INSUFFICIENT_BALANCE.getCode(), ex.getCode());
    }
}
