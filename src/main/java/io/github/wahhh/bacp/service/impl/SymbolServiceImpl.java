package io.github.wahhh.bacp.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.wahhh.bacp.common.constant.CacheKeys;
import io.github.wahhh.bacp.common.exception.BizException;
import io.github.wahhh.bacp.common.result.ResultCode;
import io.github.wahhh.bacp.common.util.JsonUtil;
import io.github.wahhh.bacp.entity.Symbol;
import io.github.wahhh.bacp.mapper.SymbolMapper;
import io.github.wahhh.bacp.service.SymbolService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Loads {@link Symbol} definitions from MySQL with a short Redis JSON cache to reduce hot-path DB reads.
 */
@Service
@RequiredArgsConstructor
public class SymbolServiceImpl implements SymbolService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final SymbolMapper symbolMapper;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * {@inheritDoc}
     */
    @Override
    public Symbol requireEnabled(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "symbol required");
        }
        String key = symbol.trim().toUpperCase();
        String redisKey = CacheKeys.TRADE_SYMBOL_DEF + key;
        String cached = stringRedisTemplate.opsForValue().get(redisKey);
        if (cached != null && !cached.isBlank()) {
            Symbol s = JsonUtil.fromJson(cached, Symbol.class);
            if (isEnabled(s)) {
                return s;
            }
        }
        Symbol row = symbolMapper.selectOne(Wrappers.<Symbol>lambdaQuery()
                .eq(Symbol::getSymbol, key)
                .eq(Symbol::getEnabled, 1));
        if (row == null) {
            throw new BizException(ResultCode.BIZ_ERROR, "unknown or disabled symbol: " + key);
        }
        stringRedisTemplate.opsForValue().set(redisKey, JsonUtil.toJson(row), CACHE_TTL);
        return row;
    }

    private static boolean isEnabled(Symbol s) {
        Integer en = s.getEnabled();
        return !Integer.valueOf(0).equals(en);
    }
}
