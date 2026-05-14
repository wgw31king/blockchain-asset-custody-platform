package io.github.wahhh.bacp.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.wahhh.bacp.dto.request.AssignRolesRequest;
import io.github.wahhh.bacp.entity.SysUser;
import io.github.wahhh.bacp.mapper.SysUserMapper;
import io.github.wahhh.bacp.mapper.SysUserRoleMapper;
import io.github.wahhh.bacp.testsupport.GlobalExceptionHandlerFactory;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserAdminControllerWebTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 场景：分页返回用户列表且密码哈希字段被抹除。
     */
    @Test
    void pageMasksPasswordHashes() throws Exception {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUser row = new SysUser();
        row.setId(1L);
        row.setUsername("admin");
        row.setPasswordHash("should-not-leak");
        Page<SysUser> data = new Page<>(1, 10);
        data.setRecords(List.of(row));
        data.setTotal(1);
        when(userMapper.selectPage(any(Page.class), any())).thenReturn(data);

        MockMvc mvc = mvc(userMapper, mock(SysUserRoleMapper.class), new BCryptPasswordEncoder(), txManager());

        mvc.perform(get("/api/v1/admin/users").param("current", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].username").value("admin"))
                .andExpect(jsonPath("$.data.records[0].passwordHash").doesNotExist());
    }

    /**
     * 场景：创建用户时用户名为空，返回业务错误码。
     */
    @Test
    void createRejectsBlankUsername() throws Exception {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUser body = new SysUser();
        body.setUsername(" ");
        body.setPasswordHash("x");

        MockMvc mvc = mvc(userMapper, mock(SysUserRoleMapper.class), new BCryptPasswordEncoder(), txManager());

        mvc.perform(post("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    /**
     * 场景：创建用户时缺少明文密码字段，校验失败。
     */
    @Test
    void createRejectsMissingPassword() throws Exception {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUser body = new SysUser();
        body.setUsername("newuser");

        MockMvc mvc = mvc(userMapper, mock(SysUserRoleMapper.class), new BCryptPasswordEncoder(), txManager());

        mvc.perform(post("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    /**
     * 场景：用户名重复时返回冲突错误。
     */
    @Test
    void createRejectsDuplicateUsername() throws Exception {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        when(userMapper.selectCount(any())).thenReturn(1L);
        SysUser body = new SysUser();
        body.setUsername("dup");
        body.setPasswordHash("secret");

        MockMvc mvc = mvc(userMapper, mock(SysUserRoleMapper.class), new BCryptPasswordEncoder(), txManager());

        mvc.perform(post("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409));
    }

    /**
     * 场景：创建用户成功后将密码置换为哈希且响应中不含哈希。
     */
    @Test
    void createHashesPasswordAndClearsResponse() throws Exception {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        when(userMapper.selectCount(any())).thenReturn(0L);
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        SysUser body = new SysUser();
        body.setUsername("alice");
        body.setPasswordHash("plain-secret");

        MockMvc mvc = mvc(userMapper, mock(SysUserRoleMapper.class), encoder, txManager());

        mvc.perform(post("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("alice"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        verify(userMapper).insert(any(SysUser.class));
    }

    /**
     * 场景：更新不存在的用户返回 NOT_FOUND。
     */
    @Test
    void updateReturnsNotFound() throws Exception {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        when(userMapper.selectById(9L)).thenReturn(null);

        MockMvc mvc = mvc(userMapper, mock(SysUserRoleMapper.class), new BCryptPasswordEncoder(), txManager());

        mvc.perform(put("/api/v1/admin/users/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    /**
     * 场景：修改用户名为已占用名称时返回冲突。
     */
    @Test
    void updateRejectsDuplicateUsername() throws Exception {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUser existing = new SysUser();
        existing.setId(3L);
        existing.setUsername("old");
        when(userMapper.selectById(3L)).thenReturn(existing);
        when(userMapper.selectCount(any())).thenReturn(1L);

        SysUser patch = new SysUser();
        patch.setUsername("taken");

        MockMvc mvc = mvc(userMapper, mock(SysUserRoleMapper.class), new BCryptPasswordEncoder(), txManager());

        mvc.perform(put("/api/v1/admin/users/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409));
    }

    /**
     * 场景：绑定角色时用户不存在。
     */
    @Test
    void assignRolesUserMissing() throws Exception {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        when(userMapper.selectById(8L)).thenReturn(null);

        AssignRolesRequest req = new AssignRolesRequest();
        req.setRoleIds(List.of(1L));

        MockMvc mvc = mvc(userMapper, mock(SysUserRoleMapper.class), new BCryptPasswordEncoder(), txManager());

        mvc.perform(post("/api/v1/admin/users/8/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    /**
     * 场景：绑定角色成功会清空旧关联并写入新关联。
     */
    @Test
    void assignRolesReplacesBindings() throws Exception {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUserRoleMapper roleMapper = mock(SysUserRoleMapper.class);
        SysUser existing = new SysUser();
        existing.setId(5L);
        existing.setUsername("bob");
        when(userMapper.selectById(5L)).thenReturn(existing);

        AssignRolesRequest req = new AssignRolesRequest();
        req.setRoleIds(java.util.Arrays.asList(1L, null, 2L));

        MockMvc mvc = mvc(userMapper, roleMapper, new BCryptPasswordEncoder(), txManager());

        mvc.perform(post("/api/v1/admin/users/5/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(roleMapper).delete(any());
        verify(roleMapper, times(2)).insert(any());
    }

    private static MockMvc mvc(
            SysUserMapper userMapper,
            SysUserRoleMapper roleMapper,
            PasswordEncoder encoder,
            PlatformTransactionManager tx) {
        return MockMvcBuilders.standaloneSetup(
                        new UserAdminController(userMapper, roleMapper, encoder, tx, new SimpleMeterRegistry()))
                .setControllerAdvice(GlobalExceptionHandlerFactory.create())
                .build();
    }

    private static PlatformTransactionManager txManager() {
        PlatformTransactionManager tm = mock(PlatformTransactionManager.class);
        when(tm.getTransaction(any(TransactionDefinition.class))).thenReturn(new SimpleTransactionStatus(true));
        doNothing().when(tm).commit(any(TransactionStatus.class));
        doNothing().when(tm).rollback(any(TransactionStatus.class));
        return tm;
    }
}
