# Architecture overview

BACP is a modular Spring Boot 3.2 service using **JWT stateless security**, **MyBatis-Plus** persistence, **Redis** for sessions/rate limits/token blacklist, and **RabbitMQ** for asynchronous custody/trade notifications.

## Layers

1. **API** — REST controllers under `io.github.wahhh.bacp.controller.*`, unified JSON envelope `Result` / `PageResult`.
2. **Application services** — `service.impl` orchestrates transactions, balances, wallets, auth, and risk checks.
3. **Domain persistence** — entities and mappers (`entity`, `mapper`); SQL migrations live in `sql/bacp_init.sql`.
4. **Custody** — `BlockchainRpcRegistry` builds pooled Web3j HTTP clients per chain profile; SPI hook `custody/spi` allows plugging chain indexers (e.g. lottery demo consumer).
5. **Cross-cutting** — global exception mapping (`GlobalExceptionHandler`), audit aspect (`common/audit`), Micrometer timers (`TimedServiceAspect`), HTTP counters (`ApiMetricsInterceptor`), Lua-backed `@RateLimit`.

## Observability

- Spring Boot Actuator exposes health and Prometheus metrics.
- Custom metrics include `bacp_http_requests_total`, `bacp_api_errors_total`, `bacp_service_seconds`, `bacp_chain_head_block`, `bacp_login_total`, `bacp_risk_alert_total`.
- Grafana provisioning references Prometheus datasource UID `bacp-prom` (see `grafana/provisioning`).

## Deployment shape

The provided Compose file builds the app image from the repo root, initializes MySQL with `bacp_init.sql`, and wires Redis/RabbitMQ for a laptop-sized demo environment.
