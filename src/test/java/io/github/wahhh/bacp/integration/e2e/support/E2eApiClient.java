package io.github.wahhh.bacp.integration.e2e.support;

import io.github.wahhh.bacp.common.result.Result;
import io.github.wahhh.bacp.common.result.ResultCode;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Thin wrapper around {@link TestRestTemplate} for JWT-backed JSON calls.
 */
public final class E2eApiClient {

    private final TestRestTemplate rest;

    public E2eApiClient(TestRestTemplate rest) {
        this.rest = rest;
    }

    public <T> ResponseEntity<Result<T>> exchange(
            HttpMethod method,
            String path,
            Object body,
            String bearerToken,
            ParameterizedTypeReference<Result<T>> typeRef) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (bearerToken != null && !bearerToken.isBlank()) {
            headers.setBearerAuth(bearerToken);
        }
        HttpEntity<?> entity = body == null ? new HttpEntity<>(headers) : new HttpEntity<>(body, headers);
        return rest.exchange(path, method, entity, typeRef);
    }

    public static void assertBusinessSuccess(Result<?> result) {
        if (result == null || result.getCode() != ResultCode.SUCCESS.getCode()) {
            throw new AssertionError(
                    "expected success result, got " + (result == null ? "null" : result.getCode() + ": " + result.getMessage()));
        }
    }
}
