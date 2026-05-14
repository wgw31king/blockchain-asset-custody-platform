package io.github.wahhh.bacp.monitor.notifier;

import io.github.wahhh.bacp.config.properties.BacpAlertProperties;
import io.github.wahhh.bacp.monitor.alert.AlertLevel;
import io.github.wahhh.bacp.monitor.alert.AlertNotification;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.client.RestTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class AlertChannelsTest {

    @Test
    void mailSkipsWhenDisabledWithoutTouchingJavaMailSender() {
        BacpAlertProperties props = new BacpAlertProperties();
        props.getMail().setEnabled(false);
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        JavaMailSender sender = mock(JavaMailSender.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        new MailNotifier(props, provider).send(AlertNotification.of(AlertLevel.ERROR, "t", "b"));
        verify(provider, never()).getIfAvailable();
        verify(sender, never()).send(any(MimeMessage.class));
    }

    @Test
    void wechatSkipsWhenDisabled() {
        BacpAlertProperties props = new BacpAlertProperties();
        props.getWechatWork().setEnabled(false);
        RestTemplate restTemplate = mock(RestTemplate.class);
        new WeChatWorkNotifier(props, restTemplate).send(AlertNotification.of(AlertLevel.WARN, "s", "b"));
        verify(restTemplate, never()).postForObject(anyString(), any(), eq(String.class));
    }

    @Test
    void dingTalkStubWhenEnabledDoesNotThrow() {
        BacpAlertProperties props = new BacpAlertProperties();
        props.getDingtalk().setEnabled(true);
        props.getDingtalk().setWebhook("https://example.com/hook");
        new DingTalkNotifier(props).send(AlertNotification.of(AlertLevel.WARN, "s", "b"));
    }
}
