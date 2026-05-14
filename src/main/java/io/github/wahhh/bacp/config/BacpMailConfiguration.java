package io.github.wahhh.bacp.config;

import io.github.wahhh.bacp.config.properties.BacpAlertProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * SMTP client from {@code bacp.alert.mail.*}; only active when host is configured.
 */
@Configuration(proxyBeanMethods = false)
@Conditional(BacpMailConfiguredCondition.class)
public class BacpMailConfiguration {

    /**
     * JavaMail sender for operational alerts (not Spring Boot mail auto-config).
     *
     * @param props bacp alert mail settings
     * @return mail sender
     */
    @Bean
    public JavaMailSender bacpJavaMailSender(BacpAlertProperties props) {
        BacpAlertProperties.Mail m = props.getMail();
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(m.getHost());
        sender.setPort(m.getPort());
        if (!m.getUsername().isBlank()) {
            sender.setUsername(m.getUsername());
        }
        if (!m.getPassword().isBlank()) {
            sender.setPassword(m.getPassword());
        }
        Properties jp = new Properties();
        jp.put("mail.transport.protocol", "smtp");
        jp.put("mail.smtp.auth", String.valueOf(!m.getUsername().isBlank()));
        jp.put("mail.smtp.ssl.enable", String.valueOf(m.isSsl()));
        jp.put("mail.smtp.starttls.enable", String.valueOf(m.isStartTls()));
        jp.put("mail.smtp.starttls.required", String.valueOf(m.isStartTls() && !m.isSsl()));
        jp.put("mail.debug", "false");
        sender.setJavaMailProperties(jp);
        return sender;
    }
}
