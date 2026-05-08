package io.github.wahhh.bacp.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.wahhh.bacp.entity.ChainNode;
import io.github.wahhh.bacp.mapper.ChainNodeMapper;
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

class ChainNodeAdminControllerWebTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 场景：列出链节点并按链类型、优先级排序。
     */
    @Test
    void listReturnsOrderedNodes() throws Exception {
        ChainNodeMapper mapper = mockMapper();
        ChainNode row = new ChainNode();
        row.setId(1L);
        row.setChainType("ethereum");
        row.setNodeName("n1");
        when(mapper.selectList(any())).thenReturn(List.of(row));

        MockMvc mvc = mvc(mapper);

        mvc.perform(get("/api/v1/admin/chain-nodes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].chainType").value("ethereum"));
    }

    /**
     * 场景：创建节点后返回持久化实体。
     */
    @Test
    void createInsertsRow() throws Exception {
        ChainNodeMapper mapper = mockMapper();
        ChainNode body = new ChainNode();
        body.setChainType("bsc");
        body.setNodeName("node-a");
        body.setRpcUrl("http://localhost:8545");

        MockMvc mvc = mvc(mapper);

        mvc.perform(post("/api/v1/admin/chain-nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        verify(mapper).insert(any(ChainNode.class));
    }

    /**
     * 场景：更新已存在节点并返回最新快照。
     */
    @Test
    void updateExistingReturnsFreshRow() throws Exception {
        ChainNodeMapper mapper = mockMapper();
        ChainNode existing = new ChainNode();
        existing.setId(1L);
        existing.setNodeName("old");
        ChainNode refreshed = new ChainNode();
        refreshed.setId(1L);
        refreshed.setNodeName("new-name");
        when(mapper.selectById(1L)).thenReturn(existing, refreshed);

        ChainNode body = new ChainNode();
        body.setRpcUrl("http://rpc");

        MockMvc mvc = mvc(mapper);

        mvc.perform(put("/api/v1/admin/chain-nodes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nodeName").value("new-name"));

        verify(mapper).updateById(any(ChainNode.class));
    }

    /**
     * 场景：更新不存在的节点返回 NOT_FOUND。
     */
    @Test
    void updateMissingReturns404() throws Exception {
        ChainNodeMapper mapper = mockMapper();
        when(mapper.selectById(404L)).thenReturn(null);

        ChainNode body = new ChainNode();
        body.setRpcUrl("x");

        MockMvc mvc = mvc(mapper);

        mvc.perform(put("/api/v1/admin/chain-nodes/404")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    /**
     * 场景：删除节点触发逻辑删除。
     */
    @Test
    void deleteCallsMapper() throws Exception {
        ChainNodeMapper mapper = mockMapper();

        MockMvc mvc = mvc(mapper);

        mvc.perform(delete("/api/v1/admin/chain-nodes/6")).andExpect(status().isOk());

        verify(mapper).deleteById(6L);
    }

    private static ChainNodeMapper mockMapper() {
        return mock(ChainNodeMapper.class);
    }

    private static MockMvc mvc(ChainNodeMapper mapper) {
        return MockMvcBuilders.standaloneSetup(new ChainNodeAdminController(mapper))
                .setControllerAdvice(GlobalExceptionHandlerFactory.create())
                .build();
    }
}
