package io.github.wahhh.bacp.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.wahhh.bacp.common.constant.MQConstants;
import io.github.wahhh.bacp.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Logs alert notifications (mail/webhook integrations can plug in here).
 */
@Slf4j
@Component
public class AlertMessageListener {

    /**
     * Consumes alert routing messages.
     *
     * @param payload alert payload
     */
    @RabbitListener(queues = MQConstants.QUEUE_ALERT)
    public void onMessage(String json) {
        Map<String, Object> payload = JsonUtil.fromJson(json, new TypeReference<Map<String, Object>>() {
        });
        log.warn("Alert payload received: {}", payload);
    }
}
