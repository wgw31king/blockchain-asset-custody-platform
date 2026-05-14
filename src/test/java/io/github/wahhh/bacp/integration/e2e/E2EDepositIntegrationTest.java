package io.github.wahhh.bacp.integration.e2e;

import io.github.wahhh.bacp.common.constant.MQConstants;
import io.github.wahhh.bacp.common.util.JsonUtil;
import io.github.wahhh.bacp.dto.request.DepositNotifyRequest;
import io.github.wahhh.bacp.integration.e2e.support.E2eApiClient;
import io.github.wahhh.bacp.integration.e2e.support.E2eConstants;
import io.github.wahhh.bacp.integration.e2e.support.E2eDataCleanup;
import io.github.wahhh.bacp.integration.e2e.support.E2eJsonTypes;
import io.github.wahhh.bacp.integration.e2e.support.E2eUserFlows;
import io.github.wahhh.bacp.listener.DepositMessageListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Deposit credit via REST notify and via RabbitMQ ingestion.
 */
class E2EDepositIntegrationTest extends AbstractFullStackIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @SpyBean
    private DepositMessageListener depositMessageListener;

    private E2eApiClient api;

    @BeforeEach
    void setUp() {
        api = new E2eApiClient(restTemplate);
        clearInvocations(depositMessageListener);
    }

    @AfterEach
    void tearDown() {
        E2eDataCleanup.purgeEphemeralUsers(jdbcTemplate);
    }

    @Test
    void step01_prepareUserAndWallet_step02_notifyDepositConfirmed_step03_assertBalance_viaRest() {
        String adminToken = E2eUserFlows.adminLogin(api);
        long userId = E2eUserFlows.createUserAndAssignUserRole(api, jdbcTemplate, "dep-rest", adminToken);
        String username = E2eConstants.EPHEMERAL_USER_PREFIX + "dep-rest";
        String userToken = E2eUserFlows.login(api, username, "UserPass@1");

        var w = api.exchange(
                HttpMethod.POST,
                "/api/v1/custody/wallets/ethereum/ensure",
                null,
                userToken,
                E2eJsonTypes.WALLET);
        assertEquals(HttpStatus.OK, w.getStatusCode());
        String toAddr = w.getBody().getData().getAddress();

        Long ethId = jdbcTemplate.queryForObject(
                "SELECT id FROM t_currency WHERE symbol = 'ETH' AND chain_type = 'ethereum' AND deleted = 0 ORDER BY id ASC LIMIT 1",
                Long.class);

        DepositNotifyRequest body = new DepositNotifyRequest();
        body.setUserId(userId);
        body.setCurrencyId(ethId);
        body.setChainType("ethereum");
        body.setTxHash("0xrest" + UUID.randomUUID().toString().replace("-", ""));
        body.setFromAddress("0xdeadbeef00000000000000000000000000000001");
        body.setToAddress(toAddr);
        body.setAmount(new BigDecimal("0.25"));
        body.setConfirmations(12);

        var notify = api.exchange(
                HttpMethod.POST,
                "/api/v1/custody/deposits/notify",
                body,
                adminToken,
                E2eJsonTypes.VOID);
        assertEquals(HttpStatus.OK, notify.getStatusCode());
        E2eApiClient.assertBusinessSuccess(notify.getBody());

        BigDecimal avail = jdbcTemplate.queryForObject(
                "SELECT available_amount FROM t_balance WHERE user_id = ? AND currency_id = ? AND deleted = 0",
                BigDecimal.class,
                userId,
                ethId);
        assertTrue(avail.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void step04_publishDepositViaMq_andAssert_listenerCreditsBalance() {
        String adminToken = E2eUserFlows.adminLogin(api);
        long userId = E2eUserFlows.createUserAndAssignUserRole(api, jdbcTemplate, "dep-mq", adminToken);
        String username = E2eConstants.EPHEMERAL_USER_PREFIX + "dep-mq";
        String userToken = E2eUserFlows.login(api, username, "UserPass@1");

        var w = api.exchange(
                HttpMethod.POST,
                "/api/v1/custody/wallets/ethereum/ensure",
                null,
                userToken,
                E2eJsonTypes.WALLET);
        String toAddr = w.getBody().getData().getAddress();

        Long ethId = jdbcTemplate.queryForObject(
                "SELECT id FROM t_currency WHERE symbol = 'ETH' AND chain_type = 'ethereum' AND deleted = 0 ORDER BY id ASC LIMIT 1",
                Long.class);

        DepositNotifyRequest body = new DepositNotifyRequest();
        body.setUserId(userId);
        body.setCurrencyId(ethId);
        body.setChainType("ethereum");
        body.setTxHash("0xmq" + UUID.randomUUID().toString().replace("-", ""));
        body.setFromAddress("0xdeadbeef00000000000000000000000000000002");
        body.setToAddress(toAddr);
        body.setAmount(new BigDecimal("0.1"));
        body.setConfirmations(12);

        rabbitTemplate.convertAndSend(MQConstants.EXCHANGE_TX, MQConstants.ROUTING_DEPOSIT, JsonUtil.toJson(body));

        verify(depositMessageListener, timeout(15000)).onMessage(ArgumentMatchers.anyString());

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            BigDecimal avail = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(available_amount, 0) FROM t_balance WHERE user_id = ? AND currency_id = ? AND deleted = 0",
                    BigDecimal.class,
                    userId,
                    ethId);
            assertTrue(avail.compareTo(BigDecimal.ZERO) > 0);
        });
    }
}
