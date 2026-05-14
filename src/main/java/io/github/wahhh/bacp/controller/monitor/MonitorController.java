package io.github.wahhh.bacp.controller.monitor;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.wahhh.bacp.common.result.Result;
import io.github.wahhh.bacp.config.openapi.OpenApiExamples;
import io.github.wahhh.bacp.entity.TxRecord;
import io.github.wahhh.bacp.mapper.SysUserMapper;
import io.github.wahhh.bacp.mapper.TxRecordMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@Tag(
        name = "Monitor",
        description = "Wrapped actuator-style snapshots for tooling. Requires `monitor:view`. Admin IP whitelist "
                + "applies under `/api/v1/admin/**` only — these routes live under `/api/v1/monitor`.")
@RestController
@RequestMapping("/api/v1/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final TxRecordMapper txRecordMapper;

    private final SysUserMapper sysUserMapper;

    private final HealthEndpoint healthEndpoint;

    @Operation(
            summary = "Aggregated health snapshot",
            description = "Returns Spring Boot `HealthEndpoint` aggregate (components depend on enabled probes).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Health JSON", content = @Content(mediaType = "application/json")),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated",
                    content =
                            @Content(
                                    mediaType = "application/json",
                                    examples =
                                            @ExampleObject(
                                                    name = "Unauthorized",
                                                    value = OpenApiExamples.RES_UNAUTHORIZED))),
            @ApiResponse(
                    responseCode = "403",
                    description = "Missing `monitor:view`",
                    content =
                            @Content(
                                    mediaType = "application/json",
                                    examples =
                                            @ExampleObject(
                                                    name = "Forbidden",
                                                    value = OpenApiExamples.RES_FORBIDDEN)))
    })
    @GetMapping("/health")
    @PreAuthorize("hasAuthority('monitor:view')")
    public Result<HealthComponent> health() {
        return Result.ok(healthEndpoint.health());
    }

    @Operation(
            summary = "Business KPI snapshot",
            description = "Lightweight counters: users, tx rows, deposit/withdraw splits.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Summary map", content = @Content(mediaType = "application/json")),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated",
                    content =
                            @Content(
                                    mediaType = "application/json",
                                    examples =
                                            @ExampleObject(
                                                    name = "Unauthorized",
                                                    value = OpenApiExamples.RES_UNAUTHORIZED))),
            @ApiResponse(
                    responseCode = "403",
                    description = "Missing `monitor:view`",
                    content =
                            @Content(
                                    mediaType = "application/json",
                                    examples =
                                            @ExampleObject(
                                                    name = "Forbidden",
                                                    value = OpenApiExamples.RES_FORBIDDEN)))
    })
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
