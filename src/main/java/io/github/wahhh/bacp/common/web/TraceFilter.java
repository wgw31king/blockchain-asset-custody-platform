package io.github.wahhh.bacp.common.web;

import io.github.wahhh.bacp.common.util.TraceIdUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Propagates {@code X-Trace-Id} across requests for log correlation.
 */
@Slf4j
@Component
public class TraceFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Trace-Id";

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = TraceIdUtil.getOrGenerate(request.getHeader(HEADER));
        TraceIdUtil.put(traceId);
        response.setHeader(HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            TraceIdUtil.clear();
        }
    }
}
