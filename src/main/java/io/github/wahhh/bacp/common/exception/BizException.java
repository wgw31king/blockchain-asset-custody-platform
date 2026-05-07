package io.github.wahhh.bacp.common.exception;

import io.github.wahhh.bacp.common.result.ResultCode;
import lombok.Getter;

/**
 * Domain-level runtime exception carrying an API-oriented code.
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;
    private final ResultCode rc;

    /**
     * Creates an exception from a {@link ResultCode}.
     *
     * @param rc canonical code
     */
    public BizException(ResultCode rc) {
        super(rc != null ? rc.getMessage() : ResultCode.BIZ_ERROR.getMessage());
        this.rc = rc;
        this.code = rc != null ? rc.getCode() : ResultCode.BIZ_ERROR.getCode();
    }

    /**
     * Creates an exception from a {@link ResultCode} with a custom message.
     *
     * @param rc             canonical code
     * @param overrideMsg    message override
     */
    public BizException(ResultCode rc, String overrideMsg) {
        super(overrideMsg != null ? overrideMsg : (rc != null ? rc.getMessage() : ResultCode.BIZ_ERROR.getMessage()));
        this.rc = rc;
        this.code = rc != null ? rc.getCode() : ResultCode.BIZ_ERROR.getCode();
    }

    /**
     * Creates an exception with a raw code and message (no {@link ResultCode} binding).
     *
     * @param code    error code
     * @param message error message
     */
    public BizException(int code, String message) {
        super(message);
        this.code = code;
        this.rc = null;
    }
}
