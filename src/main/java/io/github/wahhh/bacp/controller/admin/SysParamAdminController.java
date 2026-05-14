package io.github.wahhh.bacp.controller.admin;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.wahhh.bacp.common.exception.BizException;
import io.github.wahhh.bacp.common.result.Result;
import io.github.wahhh.bacp.common.result.ResultCode;
import io.github.wahhh.bacp.entity.SysParam;
import io.github.wahhh.bacp.config.openapi.OpenApiExamples;
import io.github.wahhh.bacp.mapper.SysParamMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin access to {@link SysParam}.
 */
@Tag(
        name = "Admin — System params",
        description = "Key/value config (`param:update`) + admin IP whitelist. List endpoint uses same authority.")
@RestController
@RequestMapping("/api/v1/admin/params")
@RequiredArgsConstructor
public class SysParamAdminController {

    private final SysParamMapper sysParamMapper;

    @Operation(summary = "List system parameters", description = "All params sorted by key.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Parameter rows"),
            @ApiResponse(
                    responseCode = "403",
                    content =
                            @Content(
                                    examples =
                                            @ExampleObject(name = "Forbidden", value = OpenApiExamples.RES_FORBIDDEN)))
    })
    @GetMapping
    @PreAuthorize("hasAuthority('param:update')")
    public Result<List<SysParam>> list() {
        List<SysParam> rows =
                sysParamMapper.selectList(Wrappers.<SysParam>lambdaQuery().orderByAsc(SysParam::getParamKey));
        return Result.ok(rows);
    }

    @Operation(summary = "Update system parameter")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated row"),
            @ApiResponse(
                    responseCode = "403",
                    content =
                            @Content(
                                    examples =
                                            @ExampleObject(name = "Forbidden", value = OpenApiExamples.RES_FORBIDDEN)))
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('param:update')")
    public Result<SysParam> update(
            @Parameter(description = "Parameter row id") @PathVariable Long id, @RequestBody SysParam body) {
        SysParam existing = sysParamMapper.selectById(id);
        if (existing == null) {
            throw new BizException(ResultCode.NOT_FOUND, "param not found");
        }
        body.setId(id);
        sysParamMapper.updateById(body);
        return Result.ok(sysParamMapper.selectById(id));
    }
}
