package io.github.wahhh.bacp.common.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Uniform REST envelope wrapping business payloads.
 *
 * @param <T> payload type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private int code;
    private String message;
    private T data;
    private long timestamp;

    /**
     * Builds a success result without a body.
     *
     * @param <T> declared payload type
     * @return success envelope
     */
    public static <T> Result<T> ok() {
        return Result.<T>builder()
                .code(ResultCode.SUCCESS.getCode())
                .message(ResultCode.SUCCESS.getMessage())
                .data(null)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Builds a success result carrying data.
     *
     * @param data business payload
     * @param <T>  payload type
     * @return success envelope
     */
    public static <T> Result<T> ok(T data) {
        return Result.<T>builder()
                .code(ResultCode.SUCCESS.getCode())
                .message(ResultCode.SUCCESS.getMessage())
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Builds an error result from code and message.
     *
     * @param code    error code
     * @param message error message
     * @param <T>     payload type
     * @return error envelope
     */
    public static <T> Result<T> error(int code, String message) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .data(null)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Builds an error result from a {@link ResultCode}.
     *
     * @param rc  canonical code
     * @param <T> payload type
     * @return error envelope
     */
    public static <T> Result<T> error(ResultCode rc) {
        return error(rc.getCode(), rc.getMessage());
    }
}
