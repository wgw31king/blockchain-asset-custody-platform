package io.github.wahhh.bacp.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.wahhh.bacp.entity.SysParam;
import io.github.wahhh.bacp.mapper.SysParamMapper;
import io.github.wahhh.bacp.testsupport.GlobalExceptionHandlerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SysParamAdminControllerWebTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 场景：列出系统参数并按 key 排序。
     */
    @Test
    void listReturnsParams() throws Exception {
        SysParamMapper mapper = mock(SysParamMapper.class);
        SysParam row = new SysParam();
        row.setId(1L);
        row.setParamKey("fee.trade.rate");
        row.setParamValue("0.001");
        when(mapper.selectList(any())).thenReturn(List.of(row));

        MockMvc mvc = mvc(mapper);

        mvc.perform(get("/api/v1/admin/params"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].paramKey").value("fee.trade.rate"));
    }

    /**
     * 场景：更新不存在的参数返回 NOT_FOUND。
     */
    @Test
    void updateMissingReturns404() throws Exception {
        SysParamMapper mapper = mock(SysParamMapper.class);
        when(mapper.selectById(999L)).thenReturn(null);

        SysParam body = new SysParam();
        body.setParamValue("v");

        MockMvc mvc = mvc(mapper);

        mvc.perform(put("/api/v1/admin/params/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    private static MockMvc mvc(SysParamMapper mapper) {
        return MockMvcBuilders.standaloneSetup(new SysParamAdminController(mapper))
                .setControllerAdvice(GlobalExceptionHandlerFactory.create())
                .build();
    }
}
