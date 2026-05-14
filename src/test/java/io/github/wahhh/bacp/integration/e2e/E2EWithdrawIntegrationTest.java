package io.github.wahhh.bacp.integration.e2e;

import io.github.wahhh.bacp.common.constant.MQConstants;
import io.github.wahhh.bacp.dto.request.WithdrawRequest;
import io.github.wahhh.bacp.integration.e2e.support.E2eApiClient;
import io.github.wahhh.bacp.integration.e2e.support.E2eConstants;
import io.github.wahhh.bacp.integration.e2e.support.E2eDataCleanup;
import io.github.wahhh.bacp.integration.e2e.support.E2eJsonTypes;
import io.github.wahhh.bacp.integration.e2e.support.E2eUserFlows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Withdraw submits debits balance, publishes MQ, async listener confirms {@code t_tx_record}.
 */
class E2EWithdrawIntegrationTest extends AbstractFullStackIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @SpyBean
    private RabbitTemplate rabbitTemplate;

    private E2eApiClient api;

    @BeforeEach
    void setUp() {
        api = new E2eApiClient(restTemplate);
    }

    @AfterEach
    void tearDown() {
        E2eDataCleanup.purgeEphemeralUsers(jdbcTemplate);
    }

    @Test
    void step01_seedBalance_step02_submitWithdraw_step03_assertMqSend_step04_assertTxConfirmedAndBalance() {
        String adminToken = E2eUserFlows.adminLogin(api);
        long userId = E2eUserFlows.createUserAndAssignUserRole(api, jdbcTemplate, "wd", adminToken);
        String username = E2eConstants.EPHEMERAL_USER_PREFIX + "wd";
        String userToken = E2eUserFlows.login(api, username, "UserPass@1");

        Long usdtId = jdbcTemplate.queryForObject(
                "SELECT id FROM t_currency WHERE symbol = 'USDT' AND chain_type = 'ethereum' AND deleted = 0 ORDER BY id ASC LIMIT 1",
                Long.class);

        jdbcTemplate.update(
                "INSERT INTO t_balance (user_id, currency_id, available_amount, frozen_amount, version, deleted) VALUES (?, ?, ?, 0, 0, 0)",
                userId,
                usdtId,
                new BigDecimal("10000"));

        WithdrawRequest req = new WithdrawRequest();
        req.setCurrencyId(usdtId);
        req.setChainType("ethereum");
        req.setToAddress("0x742d35Cc6634C0532925a3b844Bc454e4438f44e");
        req.setAmount(new BigDecimal("150"));

        var resp = api.exchange(HttpMethod.POST, "/api/v1/custody/withdraw", req, userToken, E2eJsonTypes.LONG_ID);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        E2eApiClient.assertBusinessSuccess(resp.getBody());
        Long txId = resp.getBody().getData();
        assertNotNull(txId);

        verify(rabbitTemplate, timeout(15000)).convertAndSend(eq(MQConstants.EXCHANGE_TX), eq(MQConstants.ROUTING_WITHDRAW), anyString());

        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
            String status = jdbcTemplate.queryForObject(
                    "SELECT status FROM t_tx_record WHERE id = ? AND deleted = 0",
                    String.class,
                    txId);
            assertEquals("CONFIRMED", status);
            String hash = jdbcTemplate.queryForObject(
                    "SELECT tx_hash FROM t_tx_record WHERE id = ? AND deleted = 0",
                    String.class,
                    txId);
            assertNotNull(hash);
            assertTrue(hash.startsWith("0x"));
        });

        BigDecimal avail = jdbcTemplate.queryForObject(
                "SELECT available_amount FROM t_balance WHERE user_id = ? AND currency_id = ? AND deleted = 0",
                BigDecimal.class,
                userId,
                usdtId);
        assertEquals(0, new BigDecimal("9850").compareTo(avail));
    }
}
