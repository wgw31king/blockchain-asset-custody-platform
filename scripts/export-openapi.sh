#!/usr/bin/env bash
# Export OpenAPI JSON for Markdown generation (Maven profile generate-api-docs).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAR="${ROOT}/target/bacp.jar"
OUT="${ROOT}/target/openapi.json"
# Avoid collisions with a locally running dev server (often :8080).
PORT="${BACP_EXPORT_PORT:-9847}"

if [[ ! -f "$JAR" ]]; then
  echo "Missing $JAR — run mvn package first." >&2
  exit 1
fi

java -Dspring.profiles.active=docgen -Dserver.port="${PORT}" -jar "$JAR" >/dev/null &
PID=$!

cleanup() {
  kill "${PID}" 2>/dev/null || true
  wait "${PID}" 2>/dev/null || true
}
trap cleanup EXIT

for _ in $(seq 1 90); do
  if curl -fsS "http://127.0.0.1:${PORT}/actuator/health" >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

curl -fsS --retry 3 --retry-delay 1 -o "${OUT}" "http://127.0.0.1:${PORT}/v3/api-docs"
echo "Wrote ${OUT}"
