package io.github.wahhh.bacp.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.wahhh.bacp.common.result.Result;
import io.github.wahhh.bacp.common.result.ResultCode;
import io.github.wahhh.bacp.common.util.SignatureUtil;
import io.github.wahhh.bacp.config.properties.BacpSecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Optional HMAC request signing gate ({@code bacp.security.request-signing.enabled}).
 */
@Component
@RequiredArgsConstructor
public class RequestSignatureFilter extends OncePerRequestFilter {

    private static final String HDR_TS = "X-Timestamp";

    private static final String HDR_NONCE = "X-Nonce";

    private static final String HDR_SIG = "X-Signature";

    private final BacpSecurityProperties securityProperties;

    private final NonceStore nonceStore;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!securityProperties.getRequestSigning().isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/v1/auth")
                || uri.startsWith("/actuator")
                || uri.startsWith("/v3/api-docs")
                || uri.startsWith("/swagger-ui")) {
            filterChain.doFilter(request, response);
            return;
        }
        String ts = request.getHeader(HDR_TS);
        String nonce = request.getHeader(HDR_NONCE);
        String sig = request.getHeader(HDR_SIG);
        String secret = securityProperties.getRequestSigning().getSecret();
        if (secret == null || secret.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }
        String body = "";
        if (request instanceof ContentCachingRequestWrapper w) {
            byte[] buf = w.getContentAsByteArray();
            body = buf.length == 0 ? "" : new String(buf, StandardCharsets.UTF_8);
        }
        long now = System.currentTimeMillis();
        long clientTs = parseLong(ts, now);
        long skewMs = securityProperties.getRequestSigning().getTtlSeconds() * 1000L;
        if (Math.abs(now - clientTs) > skewMs) {
            writeError(response, ResultCode.SIGNATURE_INVALID);
            return;
        }
        if (!nonceStore.checkAndStore(nonce, securityProperties.getRequestSigning().getTtlSeconds())) {
            writeError(response, ResultCode.NONCE_REPLAYED);
            return;
        }
        if (!SignatureUtil.verify(body, ts, nonce, secret, sig)) {
            writeError(response, ResultCode.SIGNATURE_INVALID);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static long parseLong(String ts, long fallback) {
        try {
            return Long.parseLong(ts);
        } catch (Exception ex) {
            return fallback;
        }
    }

    private void writeError(HttpServletResponse response, ResultCode code) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(code)));
    }
}
