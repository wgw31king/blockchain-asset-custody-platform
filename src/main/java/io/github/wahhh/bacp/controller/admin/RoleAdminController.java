package io.github.wahhh.bacp.controller.admin;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.wahhh.bacp.common.exception.BizException;
import io.github.wahhh.bacp.common.result.PageResult;
import io.github.wahhh.bacp.common.result.Result;
import io.github.wahhh.bacp.common.result.ResultCode;
import io.github.wahhh.bacp.dto.request.AssignPermissionsRequest;
import io.github.wahhh.bacp.entity.SysRole;
import io.github.wahhh.bacp.entity.SysRolePermission;
import io.github.wahhh.bacp.mapper.SysRoleMapper;
import io.github.wahhh.bacp.mapper.SysRolePermissionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * Admin role management.
 */
@Tag(name = "Admin — Roles")
@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
public class RoleAdminController {

    private final SysRoleMapper sysRoleMapper;

    private final SysRolePermissionMapper sysRolePermissionMapper;

    private final PlatformTransactionManager transactionManager;

    @Operation(summary = "Page roles")
    @GetMapping
    @PreAuthorize("hasAuthority('role:query')")
    public Result<PageResult<SysRole>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        Page<SysRole> page = new Page<>(current, size);
        Page<SysRole> result =
                sysRoleMapper.selectPage(page, Wrappers.<SysRole>lambdaQuery().orderByAsc(SysRole::getId));
        return Result.ok(PageResult.of(result));
    }

    @Operation(summary = "Create role")
    @PostMapping
    @PreAuthorize("hasAuthority('role:create')")
    public Result<SysRole> create(@RequestBody SysRole body) {
        if (body.getRoleCode() == null || body.getRoleCode().isBlank()) {
            throw new BizException(ResultCode.BAD_REQUEST, "roleCode required");
        }
        Long dup = sysRoleMapper.selectCount(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getRoleCode, body.getRoleCode()));
        if (dup != null && dup > 0) {
            throw new BizException(ResultCode.CONFLICT, "roleCode already exists");
        }
        body.setId(null);
        sysRoleMapper.insert(body);
        return Result.ok(body);
    }

    @Operation(summary = "Update role")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('role:update')")
    public Result<SysRole> update(@PathVariable Long id, @RequestBody SysRole body) {
        SysRole existing = sysRoleMapper.selectById(id);
        if (existing == null) {
            throw new BizException(ResultCode.NOT_FOUND, "role not found");
        }
        if (body.getRoleCode() != null && !body.getRoleCode().equals(existing.getRoleCode())) {
            Long dup = sysRoleMapper.selectCount(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getRoleCode, body.getRoleCode()));
            if (dup != null && dup > 0) {
                throw new BizException(ResultCode.CONFLICT, "roleCode already exists");
            }
            existing.setRoleCode(body.getRoleCode());
        }
        if (body.getRoleName() != null) {
            existing.setRoleName(body.getRoleName());
        }
        sysRoleMapper.updateById(existing);
        return Result.ok(sysRoleMapper.selectById(id));
    }

    @Operation(summary = "Delete role (logical)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('role:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        sysRoleMapper.deleteById(id);
        return Result.ok();
    }

    @Operation(summary = "Replace role permissions")
    @PostMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('role:update')")
    public Result<Void> assignPermissions(@PathVariable Long id, @RequestBody AssignPermissionsRequest body) {
        SysRole existing = sysRoleMapper.selectById(id);
        if (existing == null) {
            throw new BizException(ResultCode.NOT_FOUND, "role not found");
        }
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            sysRolePermissionMapper.delete(Wrappers.<SysRolePermission>lambdaQuery().eq(SysRolePermission::getRoleId, id));
            List<Long> permIds = body.getPermIds();
            if (permIds != null) {
                for (Long permId : permIds) {
                    if (permId == null) {
                        continue;
                    }
                    SysRolePermission link = new SysRolePermission();
                    link.setRoleId(id);
                    link.setPermId(permId);
                    sysRolePermissionMapper.insert(link);
                }
            }
        });
        return Result.ok();
    }
}
