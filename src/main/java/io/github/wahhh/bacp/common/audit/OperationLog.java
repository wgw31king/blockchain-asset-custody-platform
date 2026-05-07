package io.github.wahhh.bacp.common.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller or service method for asynchronous audit persistence.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface OperationLog {

    /**
     * Logical module name (e.g. custody, trade).
     *
     * @return module label
     */
    String module();

    /**
     * Action verb for permission / audit trail.
     *
     * @return action label
     */
    String action();

    /**
     * Whether to serialize method arguments into the audit row.
     *
     * @return true to record params JSON
     */
    boolean recordParams() default true;
}
