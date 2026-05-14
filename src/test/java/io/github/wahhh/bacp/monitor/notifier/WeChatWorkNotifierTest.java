package io.github.wahhh.bacp.monitor.notifier;

import io.github.wahhh.bacp.config.properties.BacpAlertProperties;
import io.github.wahhh.bacp.monitor.alert.AlertLevel;
import io.github.wahhh.bacp.monitor.alert.AlertNotification;
import io.github.wahhh.bacp.monitor.alert.WechatWorkMessageFormat;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class WeChatWorkNotifierTest {

    @Test
    void postsMarkdownMessage() {
        String url = "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=demo";
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.postForObject(eq(url), any(HttpEntity.class), eq(String.class)))
                .thenReturn("{\"errcode\":0,\"errmsg\":\"ok\"}");

        BacpAlertProperties props = new BacpAlertProperties();
        props.getWechatWork().setEnabled(true);
        props.getWechatWork().setWebhook(url);
        props.getWechatWork().setMessageFormat(WechatWorkMessageFormat.MARKDOWN);

        WeChatWorkNotifier notifier = new WeChatWorkNotifier(props, restTemplate);
        notifier.send(AlertNotification.of(AlertLevel.WARN, "Subj", "Details"));

        ArgumentCaptor<HttpEntity<String>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(eq(url), captor.capture(), eq(String.class));
        assertTrue(captor.getValue().getBody().contains("\"msgtype\":\"markdown\""));
        assertTrue(captor.getValue().getBody().contains("Subj"));
    }

    @Test
    void postsTextMessageWhenFormatText() {
        String url = "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=demo2";
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.postForObject(eq(url), any(HttpEntity.class), eq(String.class)))
                .thenReturn("{\"errcode\":0,\"errmsg\":\"ok\"}");

        BacpAlertProperties props = new BacpAlertProperties();
        props.getWechatWork().setEnabled(true);
        props.getWechatWork().setWebhook(url);
        props.getWechatWork().setMessageFormat(WechatWorkMessageFormat.TEXT);

        WeChatWorkNotifier notifier = new WeChatWorkNotifier(props, restTemplate);
        notifier.send(AlertNotification.of(AlertLevel.INFO, "Hello", "World"));

        ArgumentCaptor<HttpEntity<String>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(eq(url), captor.capture(), eq(String.class));
        assertTrue(captor.getValue().getBody().contains("\"msgtype\":\"text\""));
    }
}
