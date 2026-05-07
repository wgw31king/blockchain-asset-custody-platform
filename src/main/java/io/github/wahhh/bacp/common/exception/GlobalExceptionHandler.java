package io.github.wahhh.bacp.common.exception;

import io.github.wahhh.bacp.common.result.Result;
import io.github.wahhh.bacp.common.result.ResultCode;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Maps framework and domain exceptions to {@link Result} responses and metrics.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ObjectProvider<MeterRegistry> meterRegistryProvider;

    /**
     * Handles business exceptions.
     *
     * @param ex domain exception
     * @return error envelope
     */
    @ExceptionHandler(BizException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBizException(BizException ex) {
        return Result.error(ex.getCode(), ex.getMessage());
    }

    /**
     * Maps rate-limit violations to HTTP 429 and {@link ResultCode#RATE_LIMITED}.
     *
     * @param ex bucket exhaustion
     * @return error envelope
     */
    @ExceptionHandler(RateLimitException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public Result<Void> handleRateLimit(RateLimitException ex) {
        return Result.error(ResultCode.RATE_LIMITED);
    }

    /**
     * Handles request body validation errors.
     *
     * @param ex binding failure
     * @return validation error with field list
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<List<Map<String, String>>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<Map<String, String>> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .collect(Collectors.toList());
        return Result.<List<Map<String, String>>>builder()
                .code(ResultCode.VALIDATION_ERROR.getCode())
                .message(ResultCode.VALIDATION_ERROR.getMessage())
                .data(fields)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Handles constraint violations (e.g. method-level validation).
     *
     * @param ex constraint failure
     * @return validation error with field list
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<List<Map<String, String>>> handleConstraintViolation(ConstraintViolationException ex) {
        List<Map<String, String>> fields = ex.getConstraintViolations().stream()
                .map(this::toFieldError)
                .collect(Collectors.toList());
        return Result.<List<Map<String, String>>>builder()
                .code(ResultCode.VALIDATION_ERROR.getCode())
                .message(ResultCode.VALIDATION_ERROR.getMessage())
                .data(fields)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Maps access denied to {@link ResultCode#FORBIDDEN}.
     *
     * @param ex security denial
     * @return error envelope
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDenied(AccessDeniedException ex) {
        return Result.error(ResultCode.FORBIDDEN);
    }

    /**
     * Maps authentication failures to {@link ResultCode#UNAUTHORIZED}.
     *
     * @param ex auth failure
     * @return error envelope
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleAuthentication(AuthenticationException ex) {
        return Result.error(ResultCode.UNAUTHORIZED);
    }

    /**
     * Fallback handler that logs the stack trace and records a metrics counter.
     *
     * @param ex uncaught error
     * @return internal error envelope
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleAny(Exception ex) {
        log.error("Unhandled error", ex);
        MeterRegistry registry = meterRegistryProvider.getIfAvailable();
        if (registry != null) {
            registry.counter("bacp_api_errors_total").increment();
        }
        return Result.error(ResultCode.INTERNAL_ERROR);
    }

    private Map<String, String> toFieldError(FieldError fe) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("field", fe.getField());
        row.put("message", fe.getDefaultMessage());
        return row;
    }

    private Map<String, String> toFieldError(ConstraintViolation<?> v) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("path", v.getPropertyPath() != null ? v.getPropertyPath().toString() : "");
        row.put("message", v.getMessage());
        return row;
    }
}
