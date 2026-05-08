package io.github.wahhh.bacp.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.wahhh.bacp.common.constant.MQConstants;
import io.github.wahhh.bacp.common.util.JsonUtil;
import io.github.wahhh.bacp.monitor.alert.AlertLevel;
import io.github.wahhh.bacp.monitor.alert.AlertNotification;
import io.github.wahhh.bacp.monitor.alert.AlertNotificationService;
import io.github.wahhh.bacp.monitor.alert.WechatWorkMessageFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumes alert routing messages and forwards them to {@link AlertNotificationService}.
 *
 * <p>Expected JSON shape: {@code level} (INFO|WARN|ERROR|FATAL), {@code subject}, {@code body};
 * optional {@code dedupeKey}, {@code htmlBody}, {@code wechatFormat} (TEXT|MARKDOWN).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertMessageListener {

    private final AlertNotificationService alertNotificationService;

    /**
     * Consumes alert routing messages.
     *
     * @param json alert payload
     */
    @RabbitListener(queues = MQConstants.QUEUE_ALERT)
    public void onMessage(String json) {
        Map<String, Object> payload = JsonUtil.fromJson(json, new TypeReference<Map<String, Object>>() {
        });
        Object levelObj = payload.get("level");
        Object subjectObj = payload.get("subject");
        Object bodyObj = payload.get("body");
        if (levelObj == null || subjectObj == null || bodyObj == null) {
            log.warn("Alert payload missing level/subject/body: {}", payload);
            return;
        }
        try {
            AlertLevel level = AlertLevel.valueOf(String.valueOf(levelObj).trim().toUpperCase());
            String dedupeKey = payload.get("dedupeKey") != null ? String.valueOf(payload.get("dedupeKey")) : null;
            String htmlBody = payload.get("htmlBody") != null ? String.valueOf(payload.get("htmlBody")) : null;
            WechatWorkMessageFormat wechatFormat = null;
            if (payload.get("wechatFormat") != null) {
                wechatFormat = WechatWorkMessageFormat.valueOf(
                        String.valueOf(payload.get("wechatFormat")).trim().toUpperCase());
            }
            AlertNotification notification = new AlertNotification(
                    level,
                    String.valueOf(subjectObj),
                    String.valueOf(bodyObj),
                    htmlBody,
                    wechatFormat,
                    dedupeKey);
            alertNotificationService.notify(notification);
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid alert payload: {}", payload, ex);
        }
    }
}
