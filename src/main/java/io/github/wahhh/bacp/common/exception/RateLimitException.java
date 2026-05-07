package io.github.wahhh.bacp.common.exception;

import io.github.wahhh.bacp.common.result.ResultCode;

/**
 * Rate limiting violation; defaults to {@link ResultCode#RATE_LIMITED}.
 */
public class RateLimitException extends BizException {

    /**
     * Creates a rate-limit exception with the default code.
     */
    public RateLimitException() {
        super(ResultCode.RATE_LIMITED);
    }

    /**
     * Creates a rate-limit exception with a custom message.
     *
     * @param message detail message
     */
    public RateLimitException(String message) {
        super(ResultCode.RATE_LIMITED, message);
    }
}
