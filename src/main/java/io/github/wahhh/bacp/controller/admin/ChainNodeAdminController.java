package io.github.wahhh.bacp.controller.admin;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.wahhh.bacp.common.exception.BizException;
import io.github.wahhh.bacp.common.result.Result;
import io.github.wahhh.bacp.common.result.ResultCode;
import io.github.wahhh.bacp.entity.ChainNode;
import io.github.wahhh.bacp.mapper.ChainNodeMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin CRUD for {@link ChainNode}.
 */
@Tag(name = "Admin — Chain nodes")
@RestController
@RequestMapping("/api/v1/admin/chain-nodes")
@RequiredArgsConstructor
public class ChainNodeAdminController {

    private final ChainNodeMapper chainNodeMapper;

    @Operation(summary = "List chain nodes")
    @GetMapping
    @PreAuthorize("hasAuthority('node:query')")
    public Result<List<ChainNode>> list() {
        List<ChainNode> rows =
                chainNodeMapper.selectList(Wrappers.<ChainNode>lambdaQuery().orderByAsc(ChainNode::getChainType, ChainNode::getPriority));
        return Result.ok(rows);
    }

    @Operation(summary = "Create chain node")
    @PostMapping
    @PreAuthorize("hasAuthority('node:create')")
    public Result<ChainNode> create(@RequestBody ChainNode body) {
        body.setId(null);
        chainNodeMapper.insert(body);
        return Result.ok(body);
    }

    @Operation(summary = "Update chain node")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('node:update')")
    public Result<ChainNode> update(@PathVariable Long id, @RequestBody ChainNode body) {
        ChainNode existing = chainNodeMapper.selectById(id);
        if (existing == null) {
            throw new BizException(ResultCode.NOT_FOUND, "chain node not found");
        }
        body.setId(id);
        chainNodeMapper.updateById(body);
        return Result.ok(chainNodeMapper.selectById(id));
    }

    @Operation(summary = "Delete chain node (logical; requires node:update)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('node:update')")
    public Result<Void> delete(@PathVariable Long id) {
        chainNodeMapper.deleteById(id);
        return Result.ok();
    }
}
