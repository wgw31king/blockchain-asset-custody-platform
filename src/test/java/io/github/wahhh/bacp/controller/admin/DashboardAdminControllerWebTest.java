package io.github.wahhh.bacp.controller.admin;

import io.github.wahhh.bacp.mapper.BalanceMapper;
import io.github.wahhh.bacp.mapper.SysUserMapper;
import io.github.wahhh.bacp.mapper.TxRecordMapper;
import io.github.wahhh.bacp.testsupport.GlobalExceptionHandlerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardAdminControllerWebTest {

    /**
     * 场景：仪表盘汇总接口返回用户总数、流水总数、余额聚合与当日活跃用户占位字段。
     */
    @Test
    void summaryAggregatesCountsAndBalances() throws Exception {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        TxRecordMapper txMapper = mock(TxRecordMapper.class);
        BalanceMapper balanceMapper = mock(BalanceMapper.class);

        when(userMapper.selectCount(any())).thenAnswer(inv -> inv.getArgument(0) == null ? 11L : 3L);
        when(txMapper.selectCount(any())).thenReturn(22L);

        Map<String, Object> sumRow = new HashMap<>();
        sumRow.put("total", new BigDecimal("99.5"));
        when(balanceMapper.selectMaps(any())).thenReturn(List.of(sumRow));

        MockMvc mvc = MockMvcBuilders.standaloneSetup(new DashboardAdminController(userMapper, txMapper, balanceMapper))
                .setControllerAdvice(GlobalExceptionHandlerFactory.create())
                .build();

        mvc.perform(get("/api/v1/admin/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.usersTotal").value(11))
                .andExpect(jsonPath("$.data.txTotal").value(22))
                .andExpect(jsonPath("$.data.activeUsersToday").value(3))
                .andExpect(jsonPath("$.data.balancesSum").value(99.5));
    }
}
