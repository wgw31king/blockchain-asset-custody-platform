package io.github.wahhh.bacp.monitor.notifier;

import io.github.wahhh.bacp.config.properties.BacpAlertProperties;
import io.github.wahhh.bacp.monitor.alert.AlertLevel;
import io.github.wahhh.bacp.monitor.alert.AlertNotification;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class MailNotifierTest {

    @Test
    void sendsHtmlEmailWhenEnabled() throws Exception {
        BacpAlertProperties props = new BacpAlertProperties();
        props.getMail().setEnabled(true);
        props.getMail().setHost("smtp.example.com");
        props.getMail().setFrom("from@example.com");
        props.getMail().setTo("ops@example.com");

        JavaMailSender sender = mock(JavaMailSender.class);
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(sender.createMimeMessage()).thenReturn(mimeMessage);

        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        MailNotifier notifier = new MailNotifier(props, provider);

        notifier.send(AlertNotification.of(AlertLevel.ERROR, "Subject line", "Body text"));

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(captor.capture());
        assertTrue(captor.getValue() == mimeMessage);
    }

    @Test
    void skipsWhenDisabled() throws Exception {
        BacpAlertProperties props = new BacpAlertProperties();
        props.getMail().setEnabled(false);
        JavaMailSender sender = mock(JavaMailSender.class);
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        MailNotifier notifier = new MailNotifier(props, provider);
        notifier.send(AlertNotification.of(AlertLevel.WARN, "x", "y"));
        verify(sender, never()).send(any(MimeMessage.class));
    }
}
