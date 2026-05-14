package io.github.wahhh.bacp.config.openapi;

/**
 * Example JSON bodies for {@link io.swagger.v3.oas.annotations.media.ExampleObject} references.
 */
public final class OpenApiExamples {

    private OpenApiExamples() {}

    public static final String RES_OK_VOID =
            "{\"code\":200,\"message\":\"success\",\"data\":null,\"timestamp\":1715420400000}";

    public static final String RES_OK_LOGIN = "{\"code\":200,\"message\":\"success\",\"data\":{\"accessToken\":"
            + "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\",\"refreshToken\":\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
            + "\",\"expiresIn\":3600},\"timestamp\":1715420400000}";

    public static final String RES_VALIDATION =
            "{\"code\":1001,\"message\":\"validation error\",\"data\":[{\"field\":\"username\",\"message\":\"must not be"
                    + " blank\"}],\"timestamp\":1715420400000}";

    public static final String RES_UNAUTHORIZED =
            "{\"code\":401,\"message\":\"unauthorized\",\"data\":null,\"timestamp\":1715420400000}";

    public static final String RES_FORBIDDEN =
            "{\"code\":403,\"message\":\"forbidden\",\"data\":null,\"timestamp\":1715420400000}";

    public static final String RES_NOT_FOUND =
            "{\"code\":404,\"message\":\"not found\",\"data\":null,\"timestamp\":1715420400000}";

    public static final String RES_CONFLICT =
            "{\"code\":409,\"message\":\"conflict\",\"data\":null,\"timestamp\":1715420400000}";

    public static final String RES_BIZ_INSUFFICIENT =
            "{\"code\":2001,\"message\":\"insufficient balance\",\"data\":null,\"timestamp\":1715420400000}";

    public static final String RES_RATE_LIMITED =
            "{\"code\":4003,\"message\":\"rate limited\",\"data\":null,\"timestamp\":1715420400000}";

    public static final String RES_PAGE_USERS =
            "{\"code\":200,\"message\":\"success\",\"data\":{\"total\":2,\"pages\":1,\"current\":1,\"size\":10,"
                    + "\"records\":[{\"id\":1,\"username\":\"admin\",\"passwordHash\":null}]},\"timestamp\":1715420400000}";
}
