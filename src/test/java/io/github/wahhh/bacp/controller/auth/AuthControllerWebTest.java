package io.github.wahhh.bacp.controller.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.wahhh.bacp.dto.request.LoginRequest;
import io.github.wahhh.bacp.dto.response.LoginResponse;
import io.github.wahhh.bacp.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerWebTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void loginReturnsTokens() throws Exception {
        AuthService authService = mock(AuthService.class);
        when(authService.login(any())).thenReturn(LoginResponse.builder()
                .accessToken("a")
                .refreshToken("r")
                .expiresIn(3600)
                .build());

        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AuthController(authService)).build();

        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("Admin@123");

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("a"));

        verify(authService).login(any());
    }
}
