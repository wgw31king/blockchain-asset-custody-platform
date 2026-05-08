package io.github.wahhh.bacp.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.wahhh.bacp.entity.Currency;
import io.github.wahhh.bacp.mapper.CurrencyMapper;
import io.github.wahhh.bacp.testsupport.GlobalExceptionHandlerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CurrencyAdminControllerWebTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 场景：返回全部币种配置列表。
     */
    @Test
    void listReturnsRows() throws Exception {
        CurrencyMapper mapper = mock(CurrencyMapper.class);
        Currency row = new Currency();
        row.setId(1L);
        row.setSymbol("ETH");
        when(mapper.selectList(any())).thenReturn(List.of(row));

        MockMvc mvc = mvc(mapper);

        mvc.perform(get("/api/v1/admin/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].symbol").value("ETH"));
    }

    /**
     * 场景：同一 symbol+链上已存在币种时拒绝创建。
     */
    @Test
    void createRejectsDuplicate() throws Exception {
        CurrencyMapper mapper = mock(CurrencyMapper.class);
        when(mapper.selectCount(any())).thenReturn(1L);

        Currency body = new Currency();
        body.setSymbol("ETH");
        body.setName("Ether");
        body.setChainType("ethereum");

        MockMvc mvc = mvc(mapper);

        mvc.perform(post("/api/v1/admin/currencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409));
    }

    /**
     * 场景：更新不存在的币种。
     */
    @Test
    void updateMissingReturns404() throws Exception {
        CurrencyMapper mapper = mock(CurrencyMapper.class);
        when(mapper.selectById(8L)).thenReturn(null);

        Currency body = new Currency();
        body.setName("x");

        MockMvc mvc = mvc(mapper);

        mvc.perform(put("/api/v1/admin/currencies/8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    /**
     * 场景：删除币种。
     */
    @Test
    void deleteInvokesMapper() throws Exception {
        CurrencyMapper mapper = mock(CurrencyMapper.class);

        MockMvc mvc = mvc(mapper);

        mvc.perform(delete("/api/v1/admin/currencies/3")).andExpect(status().isOk());

        verify(mapper).deleteById(3L);
    }

    private static MockMvc mvc(CurrencyMapper mapper) {
        return MockMvcBuilders.standaloneSetup(new CurrencyAdminController(mapper))
                .setControllerAdvice(GlobalExceptionHandlerFactory.create())
                .build();
    }
}
