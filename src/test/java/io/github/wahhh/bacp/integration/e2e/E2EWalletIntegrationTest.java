package io.github.wahhh.bacp.integration.e2e;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class E2EWalletIntegrationTest extends AbstractFullStackIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private E2eApiClient api;

    private String userAccessToken;

    private String username;

    @BeforeAll
    void provisionUser() {
        api = new E2eApiClient(restTemplate);
        String adminToken = E2eUserFlows.adminLogin(api);
        E2eUserFlows.createUserAndAssignUserRole(api, jdbcTemplate, "wallet", adminToken);
        username = E2eConstants.EPHEMERAL_USER_PREFIX + "wallet";
        userAccessToken = E2eUserFlows.login(api, username, "UserPass@1");
    }

    @AfterAll
    void tearDownAll() {
        E2eDataCleanup.purgeEphemeralUsers(jdbcTemplate);
    }

    @Order(1)
    @Test
    void step01_loginSessionOkViaProfile() {
        var resp = api.exchange(HttpMethod.GET, "/api/v1/auth/me", null, userAccessToken, E2eJsonTypes.SYS_USER);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        E2eApiClient.assertBusinessSuccess(resp.getBody());
        assertEquals(username, resp.getBody().getData().getUsername());
    }

    @Order(2)
    @Test
    void step02_ensureWallet() {
        var resp = api.exchange(
                HttpMethod.POST,
                "/api/v1/custody/wallets/ethereum/ensure",
                null,
                userAccessToken,
                E2eJsonTypes.WALLET);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        E2eApiClient.assertBusinessSuccess(resp.getBody());
        assertNotNull(resp.getBody().getData().getAddress());
        assertFalse(resp.getBody().getData().getAddress().isBlank());
    }

    @Order(3)
    @Test
    void step03_getBalances() {
        api.exchange(HttpMethod.POST, "/api/v1/custody/wallets/ethereum/ensure", null, userAccessToken, E2eJsonTypes.WALLET);
        var resp = api.exchange(HttpMethod.GET, "/api/v1/custody/balances", null, userAccessToken, E2eJsonTypes.BALANCE_LIST);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        E2eApiClient.assertBusinessSuccess(resp.getBody());
        assertNotNull(resp.getBody().getData());
    }

    @Order(4)
    @Test
    void step04_walletRowExistsInDatabase() {
        var wResp = api.exchange(
                HttpMethod.POST,
                "/api/v1/custody/wallets/ethereum/ensure",
                null,
                userAccessToken,
                E2eJsonTypes.WALLET);
        Long uid = jdbcTemplate.queryForObject(
                "SELECT id FROM t_sys_user WHERE username = ? AND deleted = 0", Long.class, username);
        String addr = wResp.getBody().getData().getAddress();
        Long cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_wallet WHERE user_id = ? AND chain_type = 'ethereum' AND address = ? AND deleted = 0",
                Long.class,
                uid,
                addr);
        assertEquals(1L, cnt);
    }
}
