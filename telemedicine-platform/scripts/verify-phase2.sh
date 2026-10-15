#!/usr/bin/env bash
set -euo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
CONSUL_URL="${CONSUL_URL:-http://localhost:8500}"
FAILURES=0

ok() {
  if "$@"; then
    echo "[ OK ] $NAME"
  else
    echo "[FAIL] $NAME"
    FAILURES=$((FAILURES + 1))
  fi
}

echo ""
echo "=== Phase 2 verification ==="

for svc in config-service audit-service scheduler-service patient-bff physician-bff admin-bff gateway; do
  NAME="Container $svc running"
  status=$(docker compose ps "$svc" --format '{{.Status}}' 2>/dev/null || true)
  ok [[ "$status" == *"Up"* || "$status" == *"healthy"* ]]
done

NAME="Consul catalog has config-service"
ok curl -sf "$CONSUL_URL/v1/catalog/service/config-service" >/dev/null

NAME="Config GET video.provider"
ok curl -sf "$GATEWAY_URL/api/v1/config/video.provider" | grep -q simulator

NAME="Scheduler jobs list"
jobs=$(curl -sf "$GATEWAY_URL/api/v1/scheduler/jobs")
ok echo "$jobs" | grep -q appointment-reminders

NAME="Admin BFF config"
ok curl -sf "$GATEWAY_URL/bff/admin/config" | grep -q videoProvider

NAME="Gateway OpenAPI"
ok curl -sf "$GATEWAY_URL/v3/api-docs" | grep -q openapi

if [[ "$FAILURES" -gt 0 ]]; then
  echo ""
  echo "Phase 2 verification FAILED ($FAILURES checks)"
  exit 1
fi

echo ""
echo "Phase 2 verification PASSED"
