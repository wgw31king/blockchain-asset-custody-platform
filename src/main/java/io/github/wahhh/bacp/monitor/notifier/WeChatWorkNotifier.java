package io.github.wahhh.bacp.monitor.notifier;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.wahhh.bacp.common.util.JsonUtil;
import io.github.wahhh.bacp.config.properties.BacpAlertProperties;
import io.github.wahhh.bacp.monitor.alert.AlertChannelIds;
import io.github.wahhh.bacp.monitor.alert.AlertNotification;
import io.github.wahhh.bacp.monitor.alert.AlertRetrySupport;
import io.github.wahhh.bacp.monitor.alert.WechatWorkMessageFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * WeChat Work group robot webhook ({@link BacpAlertProperties#getWechatWork()}).
 */
@Component
public class WeChatWorkNotifier implements AlertChannel {

    private static final Logger log = LoggerFactory.getLogger(WeChatWorkNotifier.class);

    /** Conservative limit for markdown/text content length. */
    private static final int MAX_CONTENT_CHARS = 3800;

    private final BacpAlertProperties alertProperties;

    private final RestTemplate restTemplate;

    /**
     * @param alertProperties bacp alert settings
     * @param restTemplate    outbound HTTP
     */
    public WeChatWorkNotifier(BacpAlertProperties alertProperties, RestTemplate restTemplate) {
        this.alertProperties = alertProperties;
        this.restTemplate = restTemplate;
    }

    @Override
    public String getChannelId() {
        return AlertChannelIds.WECHAT_WORK;
    }

    @Override
    public void send(AlertNotification notification) {
        BacpAlertProperties.WechatWork ww = alertProperties.getWechatWork();
        if (!ww.isEnabled()) {
            log.debug("[wechat-work-notifier] skipped (disabled): {}", notification.subject());
            return;
        }
        if (ww.getWebhook() == null || ww.getWebhook().isBlank()) {
            log.warn("[wechat-work-notifier] skipped (webhook empty): {}", notification.subject());
            return;
        }
        WechatWorkMessageFormat fmt = notification.wechatFormat() != null
                ? notification.wechatFormat()
                : ww.getMessageFormat();
        Map<String, Object> payload = buildPayload(notification, fmt);
        String json = JsonUtil.toJson(payload);
        AlertRetrySupport.runWithRetry("wechat-work-webhook", () -> postWebhook(ww.getWebhook(), json));
    }

    private void postWebhook(String webhookUrl, String json) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(json, headers);
        String resp = restTemplate.postForObject(webhookUrl, entity, String.class);
        if (resp == null || resp.isBlank()) {
            throw new IllegalStateException("empty response from WeChat Work webhook");
        }
        JsonNode root = JsonUtil.mapper().readTree(resp);
        int errcode = root.path("errcode").asInt(-1);
        if (errcode != 0) {
            throw new IllegalStateException("WeChat Work API errcode=" + errcode + " body=" + resp);
        }
    }

    private static Map<String, Object> buildPayload(AlertNotification notification, WechatWorkMessageFormat fmt) {
        Map<String, Object> root = new LinkedHashMap<>();
        if (fmt == WechatWorkMessageFormat.MARKDOWN) {
            root.put("msgtype", "markdown");
            Map<String, String> markdown = new LinkedHashMap<>();
            markdown.put("content", truncate(markdownContent(notification)));
            root.put("markdown", markdown);
        } else {
            root.put("msgtype", "text");
            Map<String, String> text = new LinkedHashMap<>();
            text.put("content", truncate(textContent(notification)));
            root.put("text", text);
        }
        return root;
    }

    private static String markdownContent(AlertNotification n) {
        return "## " + safeLine(n.subject()) + "\n\n" + safeLine(n.body());
    }

    private static String textContent(AlertNotification n) {
        return safeLine(n.subject()) + "\n" + safeLine(n.body());
    }

    private static String safeLine(String s) {
        return s == null ? "" : s;
    }

    private static String truncate(String content) {
        if (content == null) {
            return "";
        }
        if (content.length() <= MAX_CONTENT_CHARS) {
            return content;
        }
        return content.substring(0, MAX_CONTENT_CHARS - 24) + "\n...(truncated)";
    }
}
