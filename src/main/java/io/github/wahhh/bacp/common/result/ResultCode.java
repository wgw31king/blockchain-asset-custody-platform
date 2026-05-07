package io.github.wahhh.bacp.common.result;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Canonical API result codes with HTTP-oriented and domain-specific values.
 */
@Getter
@RequiredArgsConstructor
public enum ResultCode {
    SUCCESS(200, "success"),
    BAD_REQUEST(400, "bad request"),
    UNAUTHORIZED(401, "unauthorized"),
    FORBIDDEN(403, "forbidden"),
    NOT_FOUND(404, "not found"),
    CONFLICT(409, "conflict"),
    TOO_MANY_REQUESTS(429, "too many requests"),
    INTERNAL_ERROR(500, "internal error"),
    BIZ_ERROR(1000, "business error"),
    VALIDATION_ERROR(1001, "validation error"),
    INSUFFICIENT_BALANCE(2001, "insufficient balance"),
    DUPLICATE_REQUEST(2002, "duplicate request"),
    CHAIN_ERROR(3001, "chain error"),
    SIGNATURE_INVALID(4001, "invalid signature"),
    NONCE_REPLAYED(4002, "nonce replayed"),
    RATE_LIMITED(4003, "rate limited"),
    RISK_BLOCKED(4004, "blocked by risk");

    private final int code;
    private final String message;
}
