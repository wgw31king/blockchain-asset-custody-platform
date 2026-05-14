package io.github.wahhh.bacp.controller.admin;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.wahhh.bacp.common.exception.BizException;
import io.github.wahhh.bacp.common.result.PageResult;
import io.github.wahhh.bacp.common.result.Result;
import io.github.wahhh.bacp.common.result.ResultCode;
import io.github.wahhh.bacp.config.openapi.OpenApiExamples;
import io.github.wahhh.bacp.dto.request.AssignRolesRequest;
import io.github.wahhh.bacp.entity.SysUser;
import io.github.wahhh.bacp.entity.SysUserRole;
import io.github.wahhh.bacp.mapper.SysUserMapper;
import io.github.wahhh.bacp.mapper.SysUserRoleMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin user management.
 */
@Tag(
        name = "Admin — Users",
        description = "User CRUD and role assignment. Requires `user:*` authorities **and** admin IP whitelist "
                + "(`bacp.security.admin-ip-whitelist`).")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final SysUserMapper sysUserMapper;

    private final SysUserRoleMapper sysUserRoleMapper;

    private final PasswordEncoder passwordEncoder;

    private final PlatformTransactionManager transactionManager;

    private final MeterRegistry meterRegistry;

    @Operation(summary = "Page users (password hash masked)", description = "MyBatis-Plus pagination; passwords nulled.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Paged users",
                    content =
                            @Content(
                                    mediaType = "application/json",
                                    examples =
                                            @ExampleObject(name = "Page", value = OpenApiExamples.RES_PAGE_USERS))),
            @ApiResponse(
                    responseCode = "403",
                    description = "Wrong IP or missing `user:query`",
                    content =
                            @Content(
                                    mediaType = "application/json",
                                    examples =
                                            @ExampleObject(name = "Forbidden", value = OpenApiExamples.RES_FORBIDDEN)))
    })
    @GetMapping
    @PreAuthorize("hasAuthority('user:query')")
    public Result<PageResult<SysUser>> page(
            @Parameter(description = "1-based page index") @RequestParam(defaultValue = "1") long current,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") long size) {
        Page<SysUser> page = new Page<>(current, size);
        Page<SysUser> result =
                sysUserMapper.selectPage(page, Wrappers.<SysUser>lambdaQuery().orderByDesc(SysUser::getId));
        result.getRecords().forEach(u -> u.setPasswordHash(null));
        return Result.ok(PageResult.of(result));
    }

    @Operation(
            summary = "Create user",
            description =
                    "On create, field passwordHash must carry the plaintext password; it is stored as a BCrypt hash. "
                            + "HTTP 200 with `code` 409 when username exists.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Created user (password cleared in response)"),
            @ApiResponse(
                    responseCode = "403",
                    description = "Admin IP or authority denial",
                    content =
                            @Content(
                                    examples =
                                            @ExampleObject(name = "Forbidden", value = OpenApiExamples.RES_FORBIDDEN)))
    })
    @PostMapping
    @PreAuthorize("hasAuthority('user:create')")
    public Result<SysUser> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            description = "New user; passwordHash = plaintext password (stored hashed)")
                    @RequestBody
                    SysUser body) {
        if (body.getUsername() == null || body.getUsername().isBlank()) {
            throw new BizException(ResultCode.BAD_REQUEST, "username required");
        }
        if (body.getPasswordHash() == null || body.getPasswordHash().isBlank()) {
            throw new BizException(ResultCode.BAD_REQUEST, "passwordHash (plaintext) required");
        }
        Long dup = sysUserMapper.selectCount(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, body.getUsername()));
        if (dup != null && dup > 0) {
            throw new BizException(ResultCode.CONFLICT, "username already exists");
        }
        body.setId(null);
        body.setPasswordHash(passwordEncoder.encode(body.getPasswordHash()));
        sysUserMapper.insert(body);
        // Prometheus: bacp_user_registration_total{source="ADMIN_API"}
        meterRegistry.counter("bacp_user_registration_total", "source", "ADMIN_API").increment();
        body.setPasswordHash(null);
        return Result.ok(body);
    }

    @Operation(
            summary = "Update user",
            description =
                    "If passwordHash is non-empty, it is treated as plaintext and re-hashed; omit or leave blank to keep the existing password.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated user"),
            @ApiResponse(
                    responseCode = "403",
                    description = "Admin IP or authority denial",
                    content =
                            @Content(
                                    examples =
                                            @ExampleObject(name = "Forbidden", value = OpenApiExamples.RES_FORBIDDEN)))
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user:update')")
    public Result<SysUser> update(
            @Parameter(description = "User id") @PathVariable Long id,
            @Schema(description = "passwordHash: optional plaintext to rotate password")
                    @RequestBody
                    SysUser body) {
        SysUser existing = sysUserMapper.selectById(id);
        if (existing == null) {
            throw new BizException(ResultCode.NOT_FOUND, "user not found");
        }
        if (body.getUsername() != null && !body.getUsername().equals(existing.getUsername())) {
            Long dup = sysUserMapper.selectCount(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, body.getUsername()));
            if (dup != null && dup > 0) {
                throw new BizException(ResultCode.CONFLICT, "username already exists");
            }
            existing.setUsername(body.getUsername());
        }
        if (body.getEmail() != null) {
            existing.setEmail(body.getEmail());
        }
        if (body.getNickname() != null) {
            existing.setNickname(body.getNickname());
        }
        if (body.getStatus() != null) {
            existing.setStatus(body.getStatus());
        }
        if (body.getPasswordHash() != null && !body.getPasswordHash().isBlank()) {
            existing.setPasswordHash(passwordEncoder.encode(body.getPasswordHash()));
        }
        sysUserMapper.updateById(existing);
        existing.setPasswordHash(null);
        return Result.ok(existing);
    }

    @Operation(summary = "Replace user roles", description = "Replaces all role links; empty list clears roles.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Roles updated",
                    content =
                            @Content(
                                    examples = @ExampleObject(name = "Ok", value = OpenApiExamples.RES_OK_VOID))),
            @ApiResponse(
                    responseCode = "403",
                    description = "Admin IP or authority denial",
                    content =
                            @Content(
                                    examples =
                                            @ExampleObject(name = "Forbidden", value = OpenApiExamples.RES_FORBIDDEN)))
    })
    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('user:update')")
    public Result<Void> assignRoles(
            @Parameter(description = "User id") @PathVariable Long id, @RequestBody AssignRolesRequest body) {
        SysUser existing = sysUserMapper.selectById(id);
        if (existing == null) {
            throw new BizException(ResultCode.NOT_FOUND, "user not found");
        }
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            sysUserRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, id));
            List<Long> roleIds = body.getRoleIds();
            if (roleIds != null) {
                for (Long roleId : roleIds) {
                    if (roleId == null) {
                        continue;
                    }
                    SysUserRole link = new SysUserRole();
                    link.setUserId(id);
                    link.setRoleId(roleId);
                    sysUserRoleMapper.insert(link);
                }
            }
        });
        return Result.ok();
    }
}
