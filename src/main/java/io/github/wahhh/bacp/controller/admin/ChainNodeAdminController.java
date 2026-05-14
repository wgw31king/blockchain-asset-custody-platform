package io.github.wahhh.bacp.controller.admin;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.wahhh.bacp.common.exception.BizException;
import io.github.wahhh.bacp.common.result.Result;
import io.github.wahhh.bacp.common.result.ResultCode;
import io.github.wahhh.bacp.entity.ChainNode;
import io.github.wahhh.bacp.config.openapi.OpenApiExamples;
import io.github.wahhh.bacp.mapper.ChainNodeMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@Tag(
        name = "Admin — Chain nodes",
        description = "RPC endpoints registry (`node:*`) + admin IP whitelist. Delete uses `node:update`.")
@RestController
@RequestMapping("/api/v1/admin/chain-nodes")
@RequiredArgsConstructor
public class ChainNodeAdminController {

    private final ChainNodeMapper chainNodeMapper;

    @Operation(summary = "List chain nodes")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nodes sorted by chain and priority"),
            @ApiResponse(
                    responseCode = "403",
                    content =
                            @Content(
                                    examples =
                                            @ExampleObject(name = "Forbidden", value = OpenApiExamples.RES_FORBIDDEN)))
    })
    @GetMapping
    @PreAuthorize("hasAuthority('node:query')")
    public Result<List<ChainNode>> list() {
        List<ChainNode> rows =
                chainNodeMapper.selectList(Wrappers.<ChainNode>lambdaQuery().orderByAsc(ChainNode::getChainType, ChainNode::getPriority));
        return Result.ok(rows);
    }

    @Operation(summary = "Create chain node")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Created node"),
            @ApiResponse(
                    responseCode = "403",
                    content =
                            @Content(
                                    examples =
                                            @ExampleObject(name = "Forbidden", value = OpenApiExamples.RES_FORBIDDEN)))
    })
    @PostMapping
    @PreAuthorize("hasAuthority('node:create')")
    public Result<ChainNode> create(@RequestBody ChainNode body) {
        body.setId(null);
        chainNodeMapper.insert(body);
        return Result.ok(body);
    }

    @Operation(summary = "Update chain node")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated node"),
            @ApiResponse(
                    responseCode = "403",
                    content =
                            @Content(
                                    examples =
                                            @ExampleObject(name = "Forbidden", value = OpenApiExamples.RES_FORBIDDEN)))
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('node:update')")
    public Result<ChainNode> update(
            @Parameter(description = "Node id") @PathVariable Long id, @RequestBody ChainNode body) {
        ChainNode existing = chainNodeMapper.selectById(id);
        if (existing == null) {
            throw new BizException(ResultCode.NOT_FOUND, "chain node not found");
        }
        body.setId(id);
        chainNodeMapper.updateById(body);
        return Result.ok(chainNodeMapper.selectById(id));
    }

    @Operation(summary = "Delete chain node (logical; requires node:update)")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    content =
                            @Content(
                                    examples = @ExampleObject(name = "Ok", value = OpenApiExamples.RES_OK_VOID))),
            @ApiResponse(
                    responseCode = "403",
                    content =
                            @Content(
                                    examples =
                                            @ExampleObject(name = "Forbidden", value = OpenApiExamples.RES_FORBIDDEN)))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('node:update')")
    public Result<Void> delete(@Parameter(description = "Node id") @PathVariable Long id) {
        chainNodeMapper.deleteById(id);
        return Result.ok();
    }
}
