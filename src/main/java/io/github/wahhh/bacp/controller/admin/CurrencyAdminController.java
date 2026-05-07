package io.github.wahhh.bacp.controller.admin;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.wahhh.bacp.common.exception.BizException;
import io.github.wahhh.bacp.common.result.Result;
import io.github.wahhh.bacp.common.result.ResultCode;
import io.github.wahhh.bacp.entity.Currency;
import io.github.wahhh.bacp.mapper.CurrencyMapper;
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
 * Admin CRUD for {@link Currency}.
 */
@Tag(name = "Admin — Currencies")
@RestController
@RequestMapping("/api/v1/admin/currencies")
@RequiredArgsConstructor
public class CurrencyAdminController {

    private final CurrencyMapper currencyMapper;

    @Operation(summary = "List currencies")
    @GetMapping
    @PreAuthorize("hasAuthority('currency:query')")
    public Result<List<Currency>> list() {
        List<Currency> rows =
                currencyMapper.selectList(Wrappers.<Currency>lambdaQuery().orderByAsc(Currency::getId));
        return Result.ok(rows);
    }

    @Operation(summary = "Create currency")
    @PostMapping
    @PreAuthorize("hasAuthority('currency:create')")
    public Result<Currency> create(@RequestBody Currency body) {
        body.setId(null);
        Long dup = currencyMapper.selectCount(Wrappers.<Currency>lambdaQuery()
                .eq(Currency::getSymbol, body.getSymbol())
                .eq(Currency::getChainType, body.getChainType()));
        if (dup != null && dup > 0) {
            throw new BizException(ResultCode.CONFLICT, "currency already exists for symbol and chain");
        }
        currencyMapper.insert(body);
        return Result.ok(body);
    }

    @Operation(summary = "Update currency")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('currency:update')")
    public Result<Currency> update(@PathVariable Long id, @RequestBody Currency body) {
        Currency existing = currencyMapper.selectById(id);
        if (existing == null) {
            throw new BizException(ResultCode.NOT_FOUND, "currency not found");
        }
        body.setId(id);
        currencyMapper.updateById(body);
        return Result.ok(currencyMapper.selectById(id));
    }

    @Operation(summary = "Delete currency (logical)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('currency:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        currencyMapper.deleteById(id);
        return Result.ok();
    }
}
