package io.github.wahhh.bacp.config.security;

import io.github.wahhh.bacp.common.util.IpUtil;
import io.github.wahhh.bacp.config.properties.BacpSecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Restricts {@code /api/v1/admin/**} to configured IP ranges.
 */
@Component
@RequiredArgsConstructor
public class AdminIpWhitelistFilter extends OncePerRequestFilter {

    private final BacpSecurityProperties securityProperties;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (!uri.startsWith("/api/v1/admin")) {
            filterChain.doFilter(request, response);
            return;
        }
        Set<String> allowed = Arrays.stream(securityProperties.getAdminIpWhitelist().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        String ip = IpUtil.getClientIp(request);
        if (!allowed.contains(ip)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            String body = "{\"code\":403,\"message\":\"admin ip not allowed\"}";
            response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
