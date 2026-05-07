package io.github.wahhh.bacp.common.exception;

import io.github.wahhh.bacp.common.result.ResultCode;

/**
 * Authentication failure; defaults to {@link ResultCode#UNAUTHORIZED}.
 */
public class AuthException extends BizException {

    /**
     * Creates an auth exception with the default unauthorized code.
     */
    public AuthException() {
        super(ResultCode.UNAUTHORIZED);
    }

    /**
     * Creates an auth exception with a custom message while retaining {@link ResultCode#UNAUTHORIZED}.
     *
     * @param message detail message
     */
    public AuthException(String message) {
        super(ResultCode.UNAUTHORIZED, message);
    }
}
