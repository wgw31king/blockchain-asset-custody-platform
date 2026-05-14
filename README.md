# Blockchain Asset Custody Platform (BACP)

Production-style Spring Boot backend for **custodied wallets**, **spot trading (demo matcher)**, **withdraw risk checks**, **JWT + RBAC**, **Redis rate limits**, **RabbitMQ**, and **Prometheus/Grafana** observability.

## Requirements

- **JDK 17** (Temurin recommended)
- **Maven 3.9+**
- Optional stack via Docker: MySQL 8, Redis 7, RabbitMQ 3.12 (see Compose below)
- **Docker** **29.4.3** or newer (build `055a478ea9` validated locally). Testcontainers needs a daemon whose API is **≥ 1.40** (older Docker clients against newer engines can fail with “client version … too old”).

## Quick start (local JVM)

```bash
export JAVA_HOME=/path/to/jdk-17
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
- **`bacp.alert.*`**: SMTP mail, WeChat Work webhook, routing/throttle; DingTalk remains log-only unless extended.

Copy `.env.example` when wiring CI or Compose overrides.

## Testing and coverage

```bash
mvn -B clean verify
```

Default `verify` **skips** tests tagged **`docker`** (Testcontainers) so machines without Docker still pass CI locally. To run **everything**, including `TcAdminAndRiskIntegrationTest` (**MySQL 8** + **Redis 7** via Testcontainers):

```bash
mvn -B verify -Dsurefire.excludedGroups=
```

Use **Docker Engine / CLI 29.4.3+** (see Requirements). GitHub Actions runners ship a recent Docker; if you see API version errors, align your CLI with the daemon (`docker version`) or set **`DOCKER_API_VERSION`** to match the server.

Unit and slice tests use **JUnit 5**, **Mockito**, and **standalone MockMvc** (admin controllers + `GlobalExceptionHandler`). Docker integration tests use **`AbstractTestcontainersIntegrationTest`** with profiles **`test`** + **`tc`** (`application-tc.yml`).

### JaCoCo: gate vs full

Maven `verify` generates two HTML bundles:

| Report | Location | Role |
|--------|----------|------|
| **Gate (门禁版)** | `target/site/jacoco-gate/index.html` | Same exclude set as `jacoco:check`. CI must pass **line ≥ 60%** and **branch ≥ 45%** (`jacoco.minimum.line` / `jacoco.minimum.branch` in `pom.xml`). Still excludes integration-heavy packages (bootstrap, listeners, custody/trade/monitor controllers, deposit/withdraw facades, audit/web SPI, etc.). |
| **Full (全量版)** | `target/site/jacoco-full/index.html` | **Baseline excludes only**: `entity`, `dto`, `vo`, `**/BACPApplication*`, `**/config/**`, `**/common/constant/**`, `**/*MapperImpl*`. Use this artifact for **全量统计** and roadmap tracking. |

**Published percentages**: use the latest CI artifacts **`jacoco-report-gate`** and **`jacoco-report-full`** (`index.html` inside each); refresh after merges instead of hard-coding ratios in docs.

### Follow-up coverage plan

1. Add tests for `WithdrawServiceImpl` / `DepositFacadeImpl`, then drop their gate excludes in `jacoco-maven-plugin`.
2. Cover `bootstrap/**`, `listener/**`, `controller/custody/**`, `controller/trade/**`, `controller/monitor/**`, then remove those gate excludes step by step while keeping thresholds realistic.

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

GitHub Actions runs `mvn -B verify` on JDK 17 and uploads **both** JaCoCo HTML bundles: **`jacoco-report-gate`** and **`jacoco-report-full`** (see `.github/workflows/ci.yml`).

## License

See `LICENSE` in this repository (if present).
