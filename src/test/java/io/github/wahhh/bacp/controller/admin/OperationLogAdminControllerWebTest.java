package io.github.wahhh.bacp.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.wahhh.bacp.entity.SysOperationLog;
import io.github.wahhh.bacp.mapper.SysOperationLogMapper;
import io.github.wahhh.bacp.testsupport.GlobalExceptionHandlerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OperationLogAdminControllerWebTest {

    /**
     * 场景：分页返回操作审计日志。
     */
    @Test
    void pageReturnsLogs() throws Exception {
        SysOperationLogMapper mapper = mock(SysOperationLogMapper.class);
        SysOperationLog row = new SysOperationLog();
        row.setId(9L);
        row.setUsername("admin");
        row.setCreatedAt(LocalDateTime.now());
        Page<SysOperationLog> data = new Page<>(1, 10);
        data.setRecords(List.of(row));
        data.setTotal(1);
        when(mapper.selectPage(any(Page.class), any())).thenReturn(data);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(new OperationLogAdminController(mapper))
                .setControllerAdvice(GlobalExceptionHandlerFactory.create())
                .build();
        mvc.perform(get("/api/v1/admin/operation-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].username").value("admin"));
    }
}
