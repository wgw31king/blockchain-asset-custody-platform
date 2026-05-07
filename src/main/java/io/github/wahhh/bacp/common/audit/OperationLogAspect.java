package io.github.wahhh.bacp.common.audit;

import io.github.wahhh.bacp.common.util.IpUtil;
import io.github.wahhh.bacp.common.util.JsonUtil;
import io.github.wahhh.bacp.common.web.LoginUser;
import io.github.wahhh.bacp.common.web.SecurityHelper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Persists operator actions and increments Micrometer counters when available.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    private final ObjectProvider<MeterRegistry> meterRegistryProvider;

    /**
     * Wraps annotated methods with timing and persistence.
     *
     * @param pjp join point
     * @param ol  annotation instance
     * @return method result
     * @throws Throwable propagated failure
     */
    @Around("@annotation(ol)")
    public Object around(ProceedingJoinPoint pjp, OperationLog ol) throws Throwable {
        long start = System.currentTimeMillis();
        boolean success = true;
        String errorMsg = null;
        try {
            return pjp.proceed();
        } catch (Throwable ex) {
            success = false;
            errorMsg = ex.getMessage();
            throw ex;
        } finally {
            long duration = System.currentTimeMillis() - start;
            record(ol, pjp, success, errorMsg, duration);
        }
    }

    private void record(OperationLog ol, ProceedingJoinPoint pjp, boolean success, String errorMsg, long durationMs) {
        try {
            LoginUser user = SecurityHelper.currentUser().orElse(null);
            Long userId = user != null ? user.getUserId() : null;
            String username = user != null ? user.getUsername() : "anonymous";
            String ip = resolveIp();
            String paramsJson = "";
            if (ol.recordParams()) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("args", Arrays.toString(pjp.getArgs()));
                paramsJson = JsonUtil.toJson(payload);
            }
            JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
            if (jdbc != null) {
                jdbc.update(
                        """
                                INSERT INTO t_sys_operation_log
                                (user_id, username, module, action, params, ip, success, error_msg, duration_ms, created_at)
                                VALUES (?,?,?,?,?,?,?,?,?,?)
                                """,
                        userId,
                        username,
                        ol.module(),
                        ol.action(),
                        paramsJson,
                        ip,
                        success ? 1 : 0,
                        errorMsg,
                        durationMs,
                        LocalDateTime.now());
            }
            MeterRegistry registry = meterRegistryProvider.getIfAvailable();
            if (registry != null) {
                registry.counter(
                                "bacp_business_operation_total",
                                "module", ol.module(),
                                "action", ol.action(),
                                "result", success ? "success" : "failure")
                        .increment();
            }
        } catch (Exception ex) {
            log.warn("Failed to persist operation log: {}", ex.getMessage());
        }
    }

    private static String resolveIp() {
        RequestAttributes ra = RequestContextHolder.getRequestAttributes();
        if (ra instanceof ServletRequestAttributes sra) {
            HttpServletRequest req = sra.getRequest();
            return IpUtil.getClientIp(req);
        }
        return "";
    }
}
