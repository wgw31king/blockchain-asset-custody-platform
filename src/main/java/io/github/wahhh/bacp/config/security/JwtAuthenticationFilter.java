package io.github.wahhh.bacp.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.wahhh.bacp.common.exception.BizException;
import io.github.wahhh.bacp.common.result.Result;
import io.github.wahhh.bacp.common.result.ResultCode;
import io.github.wahhh.bacp.common.constant.SecurityConstants;
import io.github.wahhh.bacp.common.util.JwtUtil;
import io.github.wahhh.bacp.common.web.LoginUser;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Stateless JWT authentication filter.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    private final TokenBlacklistService tokenBlacklistService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(SecurityConstants.BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = header.substring(SecurityConstants.BEARER_PREFIX.length()).trim();
        try {
            Claims claims = jwtUtil.parse(token);
            String jti = claims.get("jti", String.class);
            if (tokenBlacklistService.isBlacklisted(jti)) {
                throw new BizException(ResultCode.UNAUTHORIZED, "token revoked");
            }
            Long uid = claims.get("uid", Long.class);
            String username = claims.getSubject();
            Set<String> perms = extractPermissions(claims.get("perms"));
            LoginUser principal = LoginUser.builder()
                    .userId(uid)
                    .username(username != null ? username : "")
                    .permissions(perms)
                    .build();
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (BizException ex) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(Result.error(ex.getCode(), ex.getMessage())));
            return;
        }
        filterChain.doFilter(request, response);
    }

    @SuppressWarnings("unchecked")
    private static Set<String> extractPermissions(Object raw) {
        Set<String> out = new HashSet<>();
        if (raw instanceof Collection<?> col) {
            for (Object o : col) {
                if (o != null) {
                    out.add(String.valueOf(o));
                }
            }
        }
        return out;
    }
}
