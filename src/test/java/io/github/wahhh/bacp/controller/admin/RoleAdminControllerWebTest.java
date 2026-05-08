package io.github.wahhh.bacp.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.wahhh.bacp.dto.request.AssignPermissionsRequest;
import io.github.wahhh.bacp.entity.SysRole;
import io.github.wahhh.bacp.mapper.SysRoleMapper;
import io.github.wahhh.bacp.mapper.SysRolePermissionMapper;
import io.github.wahhh.bacp.testsupport.GlobalExceptionHandlerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoleAdminControllerWebTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 场景：分页查询角色列表。
     */
    @Test
    void pageReturnsRoles() throws Exception {
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysRole row = new SysRole();
        row.setId(1L);
        row.setRoleCode("ADMIN");
        Page<SysRole> data = new Page<>(1, 10);
        data.setRecords(List.of(row));
        data.setTotal(1);
        when(roleMapper.selectPage(any(Page.class), any())).thenReturn(data);

        MockMvc mvc = mvc(roleMapper, mock(SysRolePermissionMapper.class), txManager());

        mvc.perform(get("/api/v1/admin/roles").param("current", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].roleCode").value("ADMIN"));
    }

    /**
     * 场景：创建角色时 roleCode 为空。
     */
    @Test
    void createRejectsBlankRoleCode() throws Exception {
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysRole body = new SysRole();
        body.setRoleCode(" ");

        MockMvc mvc = mvc(roleMapper, mock(SysRolePermissionMapper.class), txManager());

        mvc.perform(post("/api/v1/admin/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    /**
     * 场景：roleCode 重复。
     */
    @Test
    void createRejectsDuplicateRoleCode() throws Exception {
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        when(roleMapper.selectCount(any())).thenReturn(1L);
        SysRole body = new SysRole();
        body.setRoleCode("DUP");
        body.setRoleName("Dup");

        MockMvc mvc = mvc(roleMapper, mock(SysRolePermissionMapper.class), txManager());

        mvc.perform(post("/api/v1/admin/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409));
    }

    /**
     * 场景：更新不存在的角色。
     */
    @Test
    void updateReturnsNotFound() throws Exception {
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        when(roleMapper.selectById(99L)).thenReturn(null);

        MockMvc mvc = mvc(roleMapper, mock(SysRolePermissionMapper.class), txManager());

        mvc.perform(put("/api/v1/admin/roles/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    /**
     * 场景：删除角色（逻辑删除调用）。
     */
    @Test
    void deleteInvokesMapper() throws Exception {
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);

        MockMvc mvc = mvc(roleMapper, mock(SysRolePermissionMapper.class), txManager());

        mvc.perform(delete("/api/v1/admin/roles/4")).andExpect(status().isOk());

        verify(roleMapper).deleteById(4L);
    }

    /**
     * 场景：为不存在的角色分配权限返回 NOT_FOUND。
     */
    @Test
    void assignPermissionsRoleMissing() throws Exception {
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        when(roleMapper.selectById(7L)).thenReturn(null);

        AssignPermissionsRequest req = new AssignPermissionsRequest();
        req.setPermIds(List.of(1L));

        MockMvc mvc = mvc(roleMapper, mock(SysRolePermissionMapper.class), txManager());

        mvc.perform(post("/api/v1/admin/roles/7/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    /**
     * 场景：分配权限会清空旧映射并写入新权限 ID。
     */
    @Test
    void assignPermissionsReplacesRows() throws Exception {
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysRolePermissionMapper permMapper = mock(SysRolePermissionMapper.class);
        SysRole existing = new SysRole();
        existing.setId(2L);
        existing.setRoleCode("R");
        when(roleMapper.selectById(2L)).thenReturn(existing);

        AssignPermissionsRequest req = new AssignPermissionsRequest();
        req.setPermIds(List.of(10L));

        MockMvc mvc = mvc(roleMapper, permMapper, txManager());

        mvc.perform(post("/api/v1/admin/roles/2/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(permMapper).delete(any());
        verify(permMapper).insert(any());
    }

    private static MockMvc mvc(SysRoleMapper roleMapper, SysRolePermissionMapper permMapper, PlatformTransactionManager tx) {
        return MockMvcBuilders.standaloneSetup(new RoleAdminController(roleMapper, permMapper, tx))
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
