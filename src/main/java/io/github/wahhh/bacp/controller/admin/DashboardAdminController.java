package io.github.wahhh.bacp.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.wahhh.bacp.common.result.Result;
import io.github.wahhh.bacp.entity.Balance;
import io.github.wahhh.bacp.entity.SysUser;
import io.github.wahhh.bacp.mapper.BalanceMapper;
import io.github.wahhh.bacp.mapper.SysUserMapper;
import io.github.wahhh.bacp.mapper.TxRecordMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregated KPIs for admin dashboard.
 */
@Tag(name = "Admin — Dashboard")
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class DashboardAdminController {

    private final SysUserMapper sysUserMapper;

    private final TxRecordMapper txRecordMapper;

    private final BalanceMapper balanceMapper;

    @Operation(summary = "Dashboard KPI summary")
    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public Result<Map<String, Object>> summary() {
        Long usersTotal = sysUserMapper.selectCount(null);
        Long txTotal = txRecordMapper.selectCount(null);
        Long activeUsersToday = sysUserMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .apply("DATE(last_login_at) = CURRENT_DATE"));
        BigDecimal balancesSum = sumBalances();

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("usersTotal", usersTotal);
        map.put("txTotal", txTotal);
        map.put("activeUsersToday", activeUsersToday);
        map.put("balancesSum", balancesSum);
        return Result.ok(map);
    }

    private BigDecimal sumBalances() {
        QueryWrapper<Balance> qw = Wrappers.query();
        qw.select("IFNULL(SUM(available_amount + frozen_amount), 0) AS total");
        List<Map<String, Object>> maps = balanceMapper.selectMaps(qw);
        if (maps == null || maps.isEmpty()) {
            return BigDecimal.ZERO;
        }
        Object raw = maps.get(0).get("total");
        if (raw == null) {
            return BigDecimal.ZERO;
        }
        if (raw instanceof BigDecimal bd) {
            return bd;
        }
        return new BigDecimal(raw.toString());
    }
}
