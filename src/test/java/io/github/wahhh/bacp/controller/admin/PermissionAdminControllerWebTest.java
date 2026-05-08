package io.github.wahhh.bacp.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.wahhh.bacp.entity.SysPermission;
import io.github.wahhh.bacp.mapper.SysPermissionMapper;
import io.github.wahhh.bacp.testsupport.GlobalExceptionHandlerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PermissionAdminControllerWebTest {

    /**
     * 场景：分页查询权限字典成功。
     */
    @Test
    void pageReturnsRecords() throws Exception {
        SysPermissionMapper mapper = mock(SysPermissionMapper.class);
        SysPermission row = new SysPermission();
        row.setId(1L);
        row.setPermCode("perm:query");
        Page<SysPermission> data = new Page<>(1, 20);
        data.setRecords(List.of(row));
        data.setTotal(1);
        when(mapper.selectPage(any(Page.class), any())).thenReturn(data);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(new PermissionAdminController(mapper))
                .setControllerAdvice(GlobalExceptionHandlerFactory.create())
                .build();
        mvc.perform(get("/api/v1/admin/permissions").param("current", "1").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].permCode").value("perm:query"));
    }
}
