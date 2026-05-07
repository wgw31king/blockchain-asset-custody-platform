package io.github.wahhh.bacp.risk.ratelimit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Redis Lua token-bucket guard for public or sensitive endpoints.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface RateLimit {

    /**
     * Logical bucket key (supports SpEL). Defaults to HTTP servlet path.
     *
     * @return SpEL key expression
     */
    String key() default "";

    /** Bucket capacity override; negative inherits global default. */
    int capacity() default -1;

    /** Refill rate override; negative inherits global default. */
    double refillPerSec() default -1d;

    /** Tokens consumed per successful pass (usually 1). */
    int cost() default 1;
}
