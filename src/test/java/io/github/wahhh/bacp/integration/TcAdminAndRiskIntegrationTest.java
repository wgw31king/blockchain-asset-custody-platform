package io.github.wahhh.bacp.integration;

import io.github.wahhh.bacp.service.risk.RiskEngine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testcontainers：真实 MySQL + Redis 下的管理接口与风控引擎抽检。
 */
@AutoConfigureMockMvc
class TcAdminAndRiskIntegrationTest extends AbstractTestcontainersIntegrationTest {

    private static final long RISK_USER_ID = 91001L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RiskEngine riskEngine;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void cleanRiskCounter() {
        stringRedisTemplate.delete("bacp:risk:withdraw:" + RISK_USER_ID);
    }

    /**
     * 场景：仪表盘 KPI SQL 在 MySQL 上执行成功（含 DATE / IFNULL 语义）。
     */
    @Test
    @WithMockUser(authorities = "dashboard:view")
    void dashboardSummaryUsesMysqlDialect() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.usersTotal").exists())
                .andExpect(jsonPath("$.data.txTotal").exists())
                .andExpect(jsonPath("$.data.balancesSum").exists());
    }

    /**
     * 场景：用户分页接口读取种子数据并可序列化返回。
     */
    @Test
    @WithMockUser(authorities = "user:query")
    void usersPageLoadsFromSeedDatabase() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users").param("current", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray());
    }

    /**
     * 场景：RiskEngine 使用真实 Redis 计数，小额多次提现仍在频控上限内。
     */
    @Test
    void riskEngineAllowsSequentialWithdrawsUnderCap() {
        Assertions.assertDoesNotThrow(() -> riskEngine.validateWithdraw(RISK_USER_ID, BigDecimal.ONE));
        Assertions.assertDoesNotThrow(() -> riskEngine.validateWithdraw(RISK_USER_ID, BigDecimal.ONE));
    }
}
