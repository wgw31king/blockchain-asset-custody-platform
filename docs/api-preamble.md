# BACP API reference

This document combines **hand-maintained conventions** (below) with **generated endpoint details** produced from OpenAPI annotations via the Maven profile `generate-api-docs`.

**Interactive docs:** [Swagger UI](http://localhost:8080/swagger-ui.html) (when the app is running). **Machine-readable:** `GET /v3/api-docs`.

---

## Conventions

### Base URL

Default local base path: `http://localhost:8080`. There is no servlet context path (`server.servlet.context-path` is `/`).

### Content type

JSON only unless noted: `Content-Type: application/json`.

### Authentication

- Send `Authorization: Bearer <accessToken>` on protected routes.
- Exceptions: `POST /api/v1/auth/login` and `POST /api/v1/auth/refresh` do **not** require a bearer token.

### Response envelope: `Result<T>`

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1715420400000
}
```

| Field       | Description                                                |
| ----------- | ---------------------------------------------------------- |
| `code`      | Business status (see **Result codes**). **200** = success. |
| `message`   | Human-readable message; may be overridden for errors.      |
| `data`      | Payload; `null` when omitted or empty.                     |
| `timestamp` | Epoch milliseconds (server clock).                       |

### Pagination: `PageResult<T>`

Returned inside `data` for admin list endpoints:

| Field     | Description                     |
| --------- | ------------------------------- |
| `total`   | Total matching rows.            |
| `pages`   | Total pages.                    |
| `current` | Current page index (1-based).   |
| `size`    | Page size.                      |
| `records` | Items for this page.            |

### HTTP status vs JSON `code`

| Situation                                         | HTTP status | Typical body                                       |
| ------------------------------------------------- | ----------- | -------------------------------------------------- |
| Success                                           | 200         | `code` **200**                                     |
| `BizException` (business rule / domain error)     | **200**     | Non-success `code`, optional custom `message`      |
| Bean validation (`@Valid`)                        | **400**     | `code` **1001**, `data` lists field errors          |
| Missing / invalid JWT (Spring Security)           | **401**     | `code` **401**                                     |
| Wrong IP on `/api/v1/admin/**` (whitelist filter) | **403**     | Minimal JSON (`admin ip not allowed`)              |
| `@PreAuthorize` denied                            | **403**     | `code` **403** (`Result` envelope)                 |
| Rate limit (`RateLimitException`)                 | **429**     | `code` **4003**                                    |
| Uncaught server error                             | **500**     | `code` **500**                                     |

Admin routes under `/api/v1/admin/**` require **both** valid JWT authorities **and** a client IP allowed by `bacp.security.admin-ip-whitelist`.

---

## Result codes (`ResultCode`)

| Code | Name                  | Default `message`     | Typical use                                              |
| ---- | --------------------- | --------------------- | -------------------------------------------------------- |
| 200  | SUCCESS               | success               | Normal completion                                        |
| 400  | BAD_REQUEST           | bad request           | Invalid parameters (explicit checks)                     |
| 401  | UNAUTHORIZED          | unauthorized          | Bad/expired/revoked token                                |
| 403  | FORBIDDEN             | forbidden             | Locked account, missing authority                        |
| 404  | NOT_FOUND             | not found             | Missing entity                                           |
| 409  | CONFLICT              | conflict              | Username exists, optimistic conflicts, matcher busy      |
| 429  | TOO_MANY_REQUESTS     | too many requests     | Reserved / generic                                       |
| 500  | INTERNAL_ERROR        | internal error        | Unexpected failure                                       |
| 1000 | BIZ_ERROR             | business error        | Generic domain failure                                   |
| 1001 | VALIDATION_ERROR      | validation error      | Validation (`data` may list fields)                      |
| 2001 | INSUFFICIENT_BALANCE  | insufficient balance  | Balance / freeze operations                              |
| 2002 | DUPLICATE_REQUEST     | duplicate request     | Idempotent replay (e.g. deposits)                        |
| 3001 | CHAIN_ERROR           | chain error           | Chain / RPC integration                                  |
| 4001 | SIGNATURE_INVALID     | invalid signature     | Request signing (`bacp.security.request-signing`)        |
| 4002 | NONCE_REPLAYED        | nonce replayed        | Signed request replay                                    |
| 4003 | RATE_LIMITED          | rate limited          | Lua token bucket / login throttle                        |
| 4004 | RISK_BLOCKED          | blocked by risk       | Withdraw risk engine                                     |

Domain code paths often pass a **custom `message`** string while keeping the numeric `code` above.

---

## Actuator (not in OpenAPI)

| Method | Path                     | Notes                                      |
| ------ | ------------------------ | ------------------------------------------ |
| GET    | `/actuator/health`       | Aggregated health (detail when authorized) |
| GET    | `/actuator/health/liveness` | Liveness probe                          |
| GET    | `/actuator/prometheus` | Prometheus scrape format                   |

---

## Generated operations

The following sections are **generated** from the OpenAPI model.
