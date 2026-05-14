package io.github.wahhh.bacp.monitor.alert;

import io.github.wahhh.bacp.config.properties.BacpAlertProperties;
import io.github.wahhh.bacp.monitor.notifier.AlertChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AlertNotificationServiceTest {

    private BacpAlertProperties props;

    private AlertThrottle throttle;

    @Mock
    private AlertChannel mailChannel;

    @Mock
    private AlertChannel wechatChannel;

    private AlertNotificationService service;

    @BeforeEach
    void setUp() {
        props = new BacpAlertProperties();
        props.getThrottle().setEnabled(false);
        props.getRouting().getWarn().add("mail");
        props.getRouting().getError().add("wechat-work");
        throttle = new AlertThrottle(props);
        org.mockito.Mockito.when(mailChannel.getChannelId()).thenReturn("mail");
        org.mockito.Mockito.when(wechatChannel.getChannelId()).thenReturn("wechat-work");
        service = new AlertNotificationService(props, throttle, List.of(mailChannel, wechatChannel));
    }

    @Test
    void routesWarnToMailOnly() {
        service.notify(AlertNotification.of(AlertLevel.WARN, "s", "b"));
        verify(mailChannel).send(any(AlertNotification.class));
        verify(wechatChannel, never()).send(any());
    }

    @Test
    void routesErrorToWechatOnly() {
        service.notify(AlertNotification.of(AlertLevel.ERROR, "s", "b"));
        verify(wechatChannel).send(any(AlertNotification.class));
        verify(mailChannel, never()).send(any());
    }

    @Test
    void throttleSkipsSecondDispatchWithSameDedupeKey() {
        props.getThrottle().setEnabled(true);
        props.getThrottle().setWindowSeconds(60);
        props.getThrottle().setMaxPerWindow(1);
        props.getRouting().getInfo().add("mail");
        throttle = new AlertThrottle(props);
        service = new AlertNotificationService(props, throttle, List.of(mailChannel, wechatChannel));

        var n = new AlertNotification(AlertLevel.INFO, "s", "b", null, null, "dedupe-1");
        service.notify(n);
        service.notify(n);
        verify(mailChannel, times(1)).send(any(AlertNotification.class));
    }
}
