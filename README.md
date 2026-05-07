# Blockchain Asset Custody Platform (BACP)

Production-style Spring Boot backend for **custodied wallets**, **spot trading (demo matcher)**, **withdraw risk checks**, **JWT + RBAC**, **Redis rate limits**, **RabbitMQ**, and **Prometheus/Grafana** observability.

## Requirements

- **JDK 21** (Temurin recommended)
- **Maven 3.9+**
- Optional stack via Docker: MySQL 8, Redis 7, RabbitMQ 3.12 (see Compose below)

## Quick start (local JVM)

```bash
export JAVA_HOME=/path/to/jdk-21
mvn -B clean verify
java -jar target/bacp.jar
```

- API base URL: `http://localhost:8080`
- OpenAPI UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`
- Prometheus scrape path: `http://localhost:8080/actuator/prometheus`

Seed admin (from `sql/bacp_init.sql`): **`admin` / `Admin@123`**. Change all secrets before any real deployment.

## Docker Compose (full stack)

From repository root:

```bash
docker compose -f docker/docker-compose.yml up --build
```

Typical ports (override with env vars from `.env.example`):

| Service    | Port |
|-----------|------|
| BACP app  | 8080 |
| MySQL     | 3306 |
| Redis     | 6379 |
| RabbitMQ AMQP | 5672 |
| RabbitMQ UI   | 15672 |
| Prometheus    | 9090 |
| Grafana       | 3000 |

Grafana defaults: **`admin` / `admin`** (set `GF_SECURITY_ADMIN_*` in Compose).

## Configuration highlights

- **`bacp.security.*`**: JWT issuer/audience, admin IP whitelist for `/api/v1/admin/**`, optional HMAC request signing.
- **`bacp.custody.chains.*`**: JSON-RPC URLs for `ethereum`, `bsc`, `polygon` profiles (used by `BlockchainRpcRegistry` and `ChainLagMetricsJob`).
- **`bacp.metrics.chain-poll-ms`**: scheduler interval for chain head gauges (`bacp_chain_head_block`).
- **`bacp.ratelimit.*`**: defaults for `@RateLimit` (Redis Lua token bucket).
- **`bacp.alert.*`**: mail / DingTalk / WeChat Work **stub** notifiers (log-only unless extended).

Copy `.env.example` when wiring CI or Compose overrides.

## Testing and coverage

```bash
mvn -B clean verify
```

JaCoCo **line** gate is **≥ 60%** on a **focused bundle**: entity/DTO/vo/config/bootstrap/listeners/MQ consumers/large admin CRUD controllers and similar integration surfaces are excluded so the gate reflects **domain services, security helpers, custody RPC registry, risk, trade matcher, and REST slices under test**. See `pom.xml` (`jacoco-maven-plugin` `<excludes>`).

## Repository layout

| Path | Purpose |
|------|---------|
| `sql/bacp_init.sql` | Schema + RBAC seeds |
| `docker/` | Dockerfile + `docker-compose.yml` |
| `prometheus/` | Scrape config + alert rules |
| `grafana/` | Dashboards + provisioning |
| `docs/` | Architecture and operations notes |
| `postman/` | Starter API collection |

## CI

GitHub Actions runs `mvn -B verify` on JDK 21 and uploads the JaCoCo HTML report as an artifact (see `.github/workflows/ci.yml`).

## License

See `LICENSE` in this repository (if present).
