package io.github.wahhh.bacp.integration.e2e.support;

import io.github.wahhh.bacp.common.result.Result;
import io.github.wahhh.bacp.dto.request.AssignRolesRequest;
import io.github.wahhh.bacp.dto.request.LoginRequest;
import io.github.wahhh.bacp.dto.response.LoginResponse;
import io.github.wahhh.bacp.entity.SysUser;
import org.junit.jupiter.api.Assertions;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * Admin/user onboarding helpers for E2E scenarios.
 */
public final class E2eUserFlows {

    private E2eUserFlows() {
    }

    public static String adminLogin(E2eApiClient api) {
        LoginRequest req = new LoginRequest();
        req.setUsername(E2eConstants.ADMIN_USERNAME);
        req.setPassword(E2eConstants.ADMIN_PASSWORD);
        ResponseEntity<Result<LoginResponse>> resp =
                api.exchange(HttpMethod.POST, "/api/v1/auth/login", req, null, E2eJsonTypes.LOGIN);
        Assertions.assertEquals(HttpStatus.OK, resp.getStatusCode());
        E2eApiClient.assertBusinessSuccess(resp.getBody());
        return resp.getBody().getData().getAccessToken();
    }

    public static long createUserAndAssignUserRole(E2eApiClient api, JdbcTemplate jdbc, String usernameSuffix) {
        return createUserAndAssignUserRole(api, jdbc, usernameSuffix, adminLogin(api));
    }

    public static long createUserAndAssignUserRole(
            E2eApiClient api, JdbcTemplate jdbc, String usernameSuffix, String adminToken) {
        String username = E2eConstants.EPHEMERAL_USER_PREFIX + usernameSuffix;
        SysUser body = new SysUser();
        body.setUsername(username);
        body.setPasswordHash("UserPass@1");
        var created = api.exchange(HttpMethod.POST, "/api/v1/admin/users", body, adminToken, E2eJsonTypes.SYS_USER);
        E2eApiClient.assertBusinessSuccess(created.getBody());
        Long userId = created.getBody().getData().getId();

        Long roleId = jdbc.queryForObject(
                "SELECT id FROM t_sys_role WHERE role_code = 'USER' AND deleted = 0 ORDER BY id ASC LIMIT 1",
                Long.class);
        AssignRolesRequest assign = new AssignRolesRequest();
        assign.setRoleIds(List.of(roleId));
        var assignResp = api.exchange(
                HttpMethod.POST,
                "/api/v1/admin/users/" + userId + "/roles",
                assign,
                adminToken,
                E2eJsonTypes.VOID);
        E2eApiClient.assertBusinessSuccess(assignResp.getBody());
        return userId;
    }

    public static String login(E2eApiClient api, String username, String password) {
        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword(password);
        var resp = api.exchange(HttpMethod.POST, "/api/v1/auth/login", req, null, E2eJsonTypes.LOGIN);
        Assertions.assertEquals(HttpStatus.OK, resp.getStatusCode());
        E2eApiClient.assertBusinessSuccess(resp.getBody());
        return resp.getBody().getData().getAccessToken();
    }
}
