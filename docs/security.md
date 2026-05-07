# Security model

## Authentication

- Interactive login issues **HS256 JWT** access and refresh tokens (`JwtUtil`).
- Access tokens embed permission codes in the `perms` claim; method security uses `@PreAuthorize("hasAuthority('…')")`.
- Refresh tokens carry `typ=refresh` and are rotated in `AuthServiceImpl.refresh`.
- Logout extracts `jti` from the bearer access token and adds it to a **Redis blacklist** via `TokenBlacklistService` for the remaining TTL.

## Authorization

- RBAC relations are stored in `t_sys_user`, `t_sys_role`, `t_sys_permission`, and link tables (see `sql/bacp_init.sql`).
- Users with role code **`SUPER_ADMIN`** receive all permission codes plus `*` (`SecurityConstants.PERM_ALL`).

## Operational controls

- **`/api/v1/admin/**`** is additionally guarded by **admin IP whitelist** (`AdminIpWhitelistFilter`).
- Optional **HMAC request signing** (`RequestSignatureFilter`) uses `SignatureUtil` with replay TTL from `bacp.security.request-signing`.
- **Login lockout** tracks failures in Redis (`bacp.security.login.*`).

## Secrets

Never commit real JWT secrets or crypto master keys. Use environment variables (`BACP_JWT_SECRET`, `BACP_CRYPTO_MASTER_KEY`, etc.) as shown in `docker/docker-compose.yml` and `.env.example`.
