package io.github.wahhh.bacp.controller.monitor;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.wahhh.bacp.common.result.Result;

import io.github.wahhh.bacp.entity.TxRecord;
import io.github.wahhh.bacp.mapper.SysUserMapper;
import io.github.wahhh.bacp.mapper.TxRecordMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Operational visibility endpoints for Grafana annotations / scripting.
 */
@Tag(name = "Monitor")
@RestController
@RequestMapping("/api/v1/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final TxRecordMapper txRecordMapper;

    private final SysUserMapper sysUserMapper;

    private final HealthEndpoint healthEndpoint;

    /**
     * Surfaces Spring Boot aggregated health JSON.
     *
     * @return actuator health payload
     */
    @Operation(summary = "Aggregated health snapshot")
    @GetMapping("/health")
    @PreAuthorize("hasAuthority('monitor:view')")
    public Result<HealthComponent> health() {
        return Result.ok(healthEndpoint.health());
    }

    /**
     * Compact KPI snapshot used by external dashboards.
     *
     * @return summary map
     */
    @Operation(summary = "Business KPI snapshot")
    @GetMapping("/business-summary")
    @PreAuthorize("hasAuthority('monitor:view')")
    public Result<Map<String, Object>> summary() {
        Map<String, Object> map = new LinkedHashMap<>();
        Long users = sysUserMapper.selectCount(null);
        Long txs = txRecordMapper.selectCount(null);
        Long deposits = txRecordMapper.selectCount(Wrappers.<TxRecord>lambdaQuery().eq(TxRecord::getDirection, "DEPOSIT"));
        Long withdraws = txRecordMapper.selectCount(Wrappers.<TxRecord>lambdaQuery().eq(TxRecord::getDirection, "WITHDRAW"));
        map.put("usersTotal", users);
        map.put("txTotal", txs);
        map.put("depositTxTotal", deposits);
        map.put("withdrawTxTotal", withdraws);
        return Result.ok(map);
    }
}
