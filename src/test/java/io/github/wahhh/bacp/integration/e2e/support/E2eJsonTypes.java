package io.github.wahhh.bacp.integration.e2e.support;

import io.github.wahhh.bacp.common.result.Result;
import io.github.wahhh.bacp.dto.response.LoginResponse;
import io.github.wahhh.bacp.entity.Balance;
import io.github.wahhh.bacp.entity.SysUser;
import io.github.wahhh.bacp.entity.TradeOrder;
import io.github.wahhh.bacp.entity.Wallet;
import org.springframework.core.ParameterizedTypeReference;

import java.util.List;

public final class E2eJsonTypes {

    public static final ParameterizedTypeReference<Result<LoginResponse>> LOGIN =
            new ParameterizedTypeReference<>() { };

    public static final ParameterizedTypeReference<Result<SysUser>> SYS_USER =
            new ParameterizedTypeReference<>() { };

    public static final ParameterizedTypeReference<Result<Void>> VOID = new ParameterizedTypeReference<>() { };

    public static final ParameterizedTypeReference<Result<List<Balance>>> BALANCE_LIST =
            new ParameterizedTypeReference<>() { };

    public static final ParameterizedTypeReference<Result<Wallet>> WALLET = new ParameterizedTypeReference<>() { };

    public static final ParameterizedTypeReference<Result<Long>> LONG_ID = new ParameterizedTypeReference<>() { };

    public static final ParameterizedTypeReference<Result<TradeOrder>> TRADE_ORDER =
            new ParameterizedTypeReference<>() { };

    private E2eJsonTypes() {
    }
}
