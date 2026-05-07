package io.github.wahhh.bacp.common.exception;

import io.github.wahhh.bacp.common.result.Result;
import io.github.wahhh.bacp.common.result.ResultCode;
import io.github.wahhh.bacp.controller.auth.AuthController;
import io.github.wahhh.bacp.dto.request.LoginRequest;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.MethodParameter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    @Test
    void mapsKnownExceptions() throws Exception {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ObjectProvider<MeterRegistry> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(registry);
        GlobalExceptionHandler handler = new GlobalExceptionHandler(provider);

        Result<Void> biz = handler.handleBizException(new BizException(ResultCode.BAD_REQUEST, "x"));
        assertEquals(ResultCode.BAD_REQUEST.getCode(), biz.getCode());

        Result<Void> rl = handler.handleRateLimit(new RateLimitException());
        assertEquals(ResultCode.RATE_LIMITED.getCode(), rl.getCode());

        LoginRequest target = new LoginRequest();
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(target, "loginRequest");
        errors.rejectValue("username", "NotBlank", "required");
        Method m = AuthController.class.getDeclaredMethod("login", LoginRequest.class);
        MethodParameter param = new MethodParameter(m, 0);
        MethodArgumentNotValidException manv = new MethodArgumentNotValidException(param, errors);
        Result<List<java.util.Map<String, String>>> val = handler.handleMethodArgumentNotValid(manv);
        assertFalse(val.getData().isEmpty());

        ConstraintViolation<?> v = mock(ConstraintViolation.class);
        jakarta.validation.Path path = mock(jakarta.validation.Path.class);
        when(path.toString()).thenReturn("field");
        when(v.getPropertyPath()).thenReturn(path);
        when(v.getMessage()).thenReturn("bad");
        Set<ConstraintViolation<?>> set = Collections.singleton(v);
        Result<List<java.util.Map<String, String>>> cv = handler.handleConstraintViolation(new ConstraintViolationException(set));
        assertEquals(1, cv.getData().size());

        assertEquals(ResultCode.FORBIDDEN.getCode(), handler.handleAccessDenied(new AccessDeniedException("x")).getCode());
        assertEquals(ResultCode.UNAUTHORIZED.getCode(), handler.handleAuthentication(new AuthenticationException("x") {
        }).getCode());

        Result<Void> any = handler.handleAny(new IllegalStateException("boom"));
        assertEquals(ResultCode.INTERNAL_ERROR.getCode(), any.getCode());
        assertEquals(1.0, registry.counter("bacp_api_errors_total").count(), 0.001);
    }

    @Test
    void handleAnyWithoutMeterRegistry() {
        ObjectProvider<MeterRegistry> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        GlobalExceptionHandler handler = new GlobalExceptionHandler(provider);
        Result<Void> r = handler.handleAny(new RuntimeException("x"));
        assertNotNull(r);
    }
}
