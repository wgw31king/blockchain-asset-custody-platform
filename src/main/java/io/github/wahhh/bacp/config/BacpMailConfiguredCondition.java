package io.github.wahhh.bacp.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Mail sender bean is registered only when alerting mail is enabled and SMTP host is set.
 */
public class BacpMailConfiguredCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        boolean enabled = Boolean.parseBoolean(context.getEnvironment().getProperty("bacp.alert.mail.enabled", "false"));
        String host = context.getEnvironment().getProperty("bacp.alert.mail.host", "");
        return enabled && host != null && !host.isBlank();
    }
}
