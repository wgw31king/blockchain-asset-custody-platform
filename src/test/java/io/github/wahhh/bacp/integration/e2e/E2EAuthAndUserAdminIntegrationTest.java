package io.github.wahhh.bacp.integration.e2e;

import io.github.wahhh.bacp.dto.request.AssignRolesRequest;
import io.github.wahhh.bacp.dto.request.LoginRequest;
import io.github.wahhh.bacp.entity.SysUser;
import io.github.wahhh.bacp.integration.e2e.support.E2eApiClient;
import io.github.wahhh.bacp.integration.e2e.support.E2eConstants;
import io.github.wahhh.bacp.integration.e2e.support.E2eDataCleanup;
import io.github.wahhh.bacp.integration.e2e.support.E2eJsonTypes;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Admin-backed onboarding (maps public “register”) and password rotation (maps “change password”).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class E2EAuthAndUserAdminIntegrationTest extends AbstractFullStackIntegrationTest {

    private static final String FLOW_USERNAME_SUFFIX = "authflow";

    private static final String ROTATED_PASSWORD = "Rotated@456";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private E2eApiClient api;

    private String adminAccessToken;

    private Long createdUserId;

    private String userAccessToken;

    @BeforeAll
    void initApi() {
        api = new E2eApiClient(restTemplate);
    }

    @AfterAll
    void tearDownAll() {
        E2eDataCleanup.purgeEphemeralUsers(jdbcTemplate);
    }

    private static LoginRequest login(String user, String pass) {
        LoginRequest req = new LoginRequest();
        req.setUsername(user);
        req.setPassword(pass);
        return req;
    }

    @Order(1)
    @Test
    void step01_adminLogin() {
        var resp = api.exchange(
                HttpMethod.POST,
                "/api/v1/auth/login",
                login(E2eConstants.ADMIN_USERNAME, E2eConstants.ADMIN_PASSWORD),
                null,
                E2eJsonTypes.LOGIN);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        E2eApiClient.assertBusinessSuccess(resp.getBody());
        adminAccessToken = resp.getBody().getData().getAccessToken();
        assertNotNull(adminAccessToken);
    }

    @Order(2)
    @Test
    void step02_createUser() {
        SysUser body = new SysUser();
        body.setUsername(E2eConstants.EPHEMERAL_USER_PREFIX + FLOW_USERNAME_SUFFIX);
        body.setPasswordHash("UserPass@1");
        var resp = api.exchange(HttpMethod.POST, "/api/v1/admin/users", body, adminAccessToken, E2eJsonTypes.SYS_USER);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        E2eApiClient.assertBusinessSuccess(resp.getBody());
        createdUserId = resp.getBody().getData().getId();
        assertNotNull(createdUserId);
    }

    @Order(3)
    @Test
    void step03_assignUserRole() {
        Long roleId = jdbcTemplate.queryForObject(
                "SELECT id FROM t_sys_role WHERE role_code = 'USER' AND deleted = 0 ORDER BY id ASC LIMIT 1",
                Long.class);
        AssignRolesRequest assign = new AssignRolesRequest();
        assign.setRoleIds(List.of(roleId));
        var resp = api.exchange(
                HttpMethod.POST,
                "/api/v1/admin/users/" + createdUserId + "/roles",
                assign,
                adminAccessToken,
                E2eJsonTypes.VOID);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        E2eApiClient.assertBusinessSuccess(resp.getBody());
    }

    @Order(4)
    @Test
    void step04_loginAsNewUser() {
        var resp = api.exchange(
                HttpMethod.POST,
                "/api/v1/auth/login",
                login(E2eConstants.EPHEMERAL_USER_PREFIX + FLOW_USERNAME_SUFFIX, "UserPass@1"),
                null,
                E2eJsonTypes.LOGIN);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        E2eApiClient.assertBusinessSuccess(resp.getBody());
        userAccessToken = resp.getBody().getData().getAccessToken();
    }

    @Order(5)
    @Test
    void step05_getProfile() {
        var resp = api.exchange(HttpMethod.GET, "/api/v1/auth/me", null, userAccessToken, E2eJsonTypes.SYS_USER);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        E2eApiClient.assertBusinessSuccess(resp.getBody());
        assertEquals(E2eConstants.EPHEMERAL_USER_PREFIX + FLOW_USERNAME_SUFFIX, resp.getBody().getData().getUsername());
    }

    @Order(6)
    @Test
    void step06_adminChangesUserPassword() {
        SysUser patch = new SysUser();
        patch.setPasswordHash(ROTATED_PASSWORD);
        var resp = api.exchange(
                HttpMethod.PUT,
                "/api/v1/admin/users/" + createdUserId,
                patch,
                adminAccessToken,
                E2eJsonTypes.SYS_USER);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        E2eApiClient.assertBusinessSuccess(resp.getBody());
    }

    @Order(7)
    @Test
    void step07_loginWithNewPassword() {
        var resp = api.exchange(
                HttpMethod.POST,
                "/api/v1/auth/login",
                login(E2eConstants.EPHEMERAL_USER_PREFIX + FLOW_USERNAME_SUFFIX, ROTATED_PASSWORD),
                null,
                E2eJsonTypes.LOGIN);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        E2eApiClient.assertBusinessSuccess(resp.getBody());
        assertNotNull(resp.getBody().getData().getAccessToken());
    }
}
