package io.github.wahhh.bacp.monitor.notifier;

import io.github.wahhh.bacp.config.properties.BacpAlertProperties;
import org.junit.jupiter.api.Test;

class AlertNotifiersTest {

    @Test
    void stubsDoNotThrowWhenDisabled() {
        BacpAlertProperties props = new BacpAlertProperties();
        props.getMail().setEnabled(false);
        props.getDingtalk().setEnabled(false);
        props.getWechatWork().setEnabled(false);

        new MailNotifier(props).sendAlert("t", "b");
        new DingTalkNotifier(props).sendAlert("t", "b");
        new WeChatWorkNotifier(props).sendAlert("t", "b");
    }

    @Test
    void stubsLogWhenEnabled() {
        BacpAlertProperties props = new BacpAlertProperties();
        props.getMail().setEnabled(true);
        props.getMail().setTo("ops@example.com");
        props.getDingtalk().setEnabled(true);
        props.getDingtalk().setWebhook("https://example.com/hook");
        props.getWechatWork().setEnabled(true);
        props.getWechatWork().setWebhook("https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=demo");

        new MailNotifier(props).sendAlert("subject", "body");
        new DingTalkNotifier(props).sendAlert("subject", "body");
        new WeChatWorkNotifier(props).sendAlert("subject", "body");
    }
}
