package io.github.wahhh.bacp.integration.e2e;

import io.github.wahhh.bacp.common.enums.OrderStatus;
import io.github.wahhh.bacp.dto.request.OrderCreateRequest;
import io.github.wahhh.bacp.integration.e2e.support.E2eApiClient;
import io.github.wahhh.bacp.integration.e2e.support.E2eConstants;
import io.github.wahhh.bacp.integration.e2e.support.E2eDataCleanup;
import io.github.wahhh.bacp.integration.e2e.support.E2eJsonTypes;
import io.github.wahhh.bacp.integration.e2e.support.E2eUserFlows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two traders match on ETH-USDT; asserts orders, trade prints, and post-trade balances.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class E2ETradeMatchingIntegrationTest extends AbstractFullStackIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private E2eApiClient api;

    private String buyerToken;

    private String sellerToken;

    private long buyerId;

    private long sellerId;

    private Long ethId;

    private Long usdtId;

    private Long sellOrderId;

    private Long buyOrderId;

    @BeforeAll
    void prepareTwoTradersWithBalances() {
        api = new E2eApiClient(restTemplate);
        String adminToken = E2eUserFlows.adminLogin(api);
        buyerId = E2eUserFlows.createUserAndAssignUserRole(api, jdbcTemplate, "buyer", adminToken);
        sellerId = E2eUserFlows.createUserAndAssignUserRole(api, jdbcTemplate, "seller", adminToken);

        ethId = jdbcTemplate.queryForObject(
                "SELECT id FROM t_currency WHERE symbol = 'ETH' AND chain_type = 'ethereum' AND deleted = 0 ORDER BY id ASC LIMIT 1",
                Long.class);
        usdtId = jdbcTemplate.queryForObject(
                "SELECT id FROM t_currency WHERE symbol = 'USDT' AND chain_type = 'ethereum' AND deleted = 0 ORDER BY id ASC LIMIT 1",
                Long.class);

        jdbcTemplate.update(
                "INSERT INTO t_balance (user_id, currency_id, available_amount, frozen_amount, version, deleted) VALUES (?, ?, ?, 0, 0, 0)",
                buyerId,
                usdtId,
                new BigDecimal("1000000"));
        jdbcTemplate.update(
                "INSERT INTO t_balance (user_id, currency_id, available_amount, frozen_amount, version, deleted) VALUES (?, ?, ?, 0, 0, 0)",
                sellerId,
                ethId,
                new BigDecimal("500"));

        buyerToken = E2eUserFlows.login(api, E2eConstants.EPHEMERAL_USER_PREFIX + "buyer", "UserPass@1");
        sellerToken = E2eUserFlows.login(api, E2eConstants.EPHEMERAL_USER_PREFIX + "seller", "UserPass@1");
    }

    @AfterAll
    void tearDownAll() {
        E2eDataCleanup.purgeEphemeralUsers(jdbcTemplate);
    }

    @Order(1)
    @Test
    void step02_placeSellOrderAsMaker() {
        OrderCreateRequest req = new OrderCreateRequest();
        req.setSymbol("ETH-USDT");
        req.setSide("SELL");
        req.setOrderType("LIMIT");
        req.setPrice(new BigDecimal("2500"));
        req.setQuantity(new BigDecimal("0.05"));
        var resp = api.exchange(HttpMethod.POST, "/api/v1/orders", req, sellerToken, E2eJsonTypes.TRADE_ORDER);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        E2eApiClient.assertBusinessSuccess(resp.getBody());
        sellOrderId = resp.getBody().getData().getId();
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM t_order WHERE id = ? AND deleted = 0", String.class, sellOrderId);
        assertEquals(OrderStatus.PENDING.name(), status);
    }

    @Order(2)
    @Test
    void step03_placeBuyOrderAsTaker() {
        OrderCreateRequest req = new OrderCreateRequest();
        req.setSymbol("ETH-USDT");
        req.setSide("BUY");
        req.setOrderType("LIMIT");
        req.setPrice(new BigDecimal("2500"));
        req.setQuantity(new BigDecimal("0.05"));
        var resp = api.exchange(HttpMethod.POST, "/api/v1/orders", req, buyerToken, E2eJsonTypes.TRADE_ORDER);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        E2eApiClient.assertBusinessSuccess(resp.getBody());
        buyOrderId = resp.getBody().getData().getId();
    }

    @Order(3)
    @Test
    void step04_assertOrdersFilled() {
        String sellSt = jdbcTemplate.queryForObject(
                "SELECT status FROM t_order WHERE id = ? AND deleted = 0", String.class, sellOrderId);
        String buySt = jdbcTemplate.queryForObject(
                "SELECT status FROM t_order WHERE id = ? AND deleted = 0", String.class, buyOrderId);
        assertEquals(OrderStatus.FILLED.name(), sellSt);
        assertEquals(OrderStatus.FILLED.name(), buySt);
    }

    @Order(4)
    @Test
    void step05_assertTradeAndSettlement() {
        Long trades = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_trade WHERE symbol = 'ETH-USDT' AND buyer_id = ? AND seller_id = ? AND deleted = 0",
                Long.class,
                buyerId,
                sellerId);
        assertTrue(trades != null && trades >= 1);

        BigDecimal buyerEth = jdbcTemplate.queryForObject(
                "SELECT COALESCE(available_amount, 0) FROM t_balance WHERE user_id = ? AND currency_id = ? AND deleted = 0",
                BigDecimal.class,
                buyerId,
                ethId);
        BigDecimal sellerUsdt = jdbcTemplate.queryForObject(
                "SELECT COALESCE(available_amount, 0) FROM t_balance WHERE user_id = ? AND currency_id = ? AND deleted = 0",
                BigDecimal.class,
                sellerId,
                usdtId);

        assertTrue(buyerEth.compareTo(BigDecimal.ZERO) > 0);
        assertTrue(sellerUsdt.compareTo(BigDecimal.ZERO) > 0);

        Long flows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_capital_flow WHERE user_id IN (?, ?) AND ref_type = 'TRADE' AND deleted = 0",
                Long.class,
                buyerId,
                sellerId);
        assertTrue(flows != null && flows >= 1);
    }
}
