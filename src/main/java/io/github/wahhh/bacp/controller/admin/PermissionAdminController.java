package io.github.wahhh.bacp.controller.admin;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.wahhh.bacp.common.result.PageResult;
import io.github.wahhh.bacp.common.result.Result;
import io.github.wahhh.bacp.entity.SysPermission;
import io.github.wahhh.bacp.mapper.SysPermissionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only permission catalog for RBAC administration.
 */
@Tag(name = "Admin — Permissions")
@RestController
@RequestMapping("/api/v1/admin/permissions")
@RequiredArgsConstructor
public class PermissionAdminController {

    private final SysPermissionMapper sysPermissionMapper;

    /**
     * Pages all permission rows (button-level codes for {@code @PreAuthorize}).
     *
     * @param current page (1-based, MyBatis-Plus convention)
     * @param size    page size
     * @return paged permissions
     */
    @Operation(summary = "Page permissions")
    @GetMapping
    @PreAuthorize("hasAuthority('perm:query')")
    public Result<PageResult<SysPermission>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        Page<SysPermission> page = new Page<>(current, size);
        Page<SysPermission> data = sysPermissionMapper.selectPage(page,
                Wrappers.<SysPermission>lambdaQuery().orderByAsc(SysPermission::getId));
        return Result.ok(PageResult.of(data));
    }
}
