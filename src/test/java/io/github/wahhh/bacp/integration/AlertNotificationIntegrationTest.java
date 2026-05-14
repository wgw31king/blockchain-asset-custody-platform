package io.github.wahhh.bacp.integration;

import io.github.wahhh.bacp.monitor.alert.AlertLevel;
import io.github.wahhh.bacp.monitor.alert.AlertNotification;
import io.github.wahhh.bacp.monitor.alert.AlertNotificationService;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@link AlertNotificationService} dispatches to the mail channel with a mocked {@link JavaMailSender}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@TestPropertySource(
        properties = {
                "bacp.alert.mail.enabled=true",
                "bacp.alert.mail.host=127.0.0.1",
                "bacp.alert.mail.to=ops@test.com",
                "bacp.alert.routing.warn[0]=mail",
                "bacp.alert.throttle.enabled=false"
        })
class AlertNotificationIntegrationTest {

    @Autowired
    private AlertNotificationService alertNotificationService;

    @MockBean
    private JavaMailSender javaMailSender;

    /** Test profile excludes Redis auto-config; provide a mock for security and rate-limit beans. */
    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    /** Test profile excludes Redisson auto-config; matcher still needs a lock stub for context bootstrap. */
    @MockBean
    private RedissonClient redissonClient;

    @BeforeEach
    void stubRedissonLock() throws Exception {
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        doReturn(true).when(lock).tryLock(anyLong(), anyLong(), any(TimeUnit.class));
        when(lock.isHeldByCurrentThread()).thenReturn(true);
    }

    @Test
    void dispatchesToMailForRoutedLevel() throws Exception {
        MimeMessage mm = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mm);
        doNothing().when(javaMailSender).send(any(MimeMessage.class));

        alertNotificationService.notify(AlertNotification.of(AlertLevel.WARN, "integration", "message body"));

        verify(javaMailSender).send(any(MimeMessage.class));
    }
}
