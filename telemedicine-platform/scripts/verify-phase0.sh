#!/usr/bin/env bash
set -euo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
CONSUL_URL="${CONSUL_URL:-http://localhost:8500}"
PROMETHEUS_URL="${PROMETHEUS_URL:-http://localhost:9090}"
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8180}"
SIMULATORS_URL="${SIMULATORS_URL:-http://localhost:8200}"
REALM="${KEYCLOAK_REALM:-telemedicine}"

FAILURES=0

ok()   { echo "[ OK ] $1"; }
fail() { echo "[FAIL] $1${2:+ — $2}"; FAILURES=$((FAILURES + 1)); }

echo ""
echo "=== Phase 0 verification ==="

REQUIRED=(consul postgres redis kafka minio keycloak prometheus jaeger external-simulators gateway)
for svc in "${REQUIRED[@]}"; do
  status=$(docker compose ps "$svc" --format '{{.Status}}' 2>/dev/null || true)
  if echo "$status" | grep -qiE 'healthy|Up'; then
    ok "Container $svc running"
  else
    fail "Container $svc running" "$status"
  fi
done

if curl -sf "${CONSUL_URL}/v1/catalog/services" | grep -q '"gateway"'; then
  ok "Consul catalog has gateway"
else
  fail "Consul catalog has gateway"
fi

if curl -sf "${CONSUL_URL}/v1/catalog/services" | grep -q '"external-simulators"'; then
  ok "Consul catalog has external-simulators"
else
  fail "Consul catalog has external-simulators"
fi

if curl -sf "${GATEWAY_URL}/actuator/health" | grep -q '"status":"UP"'; then
  ok "Gateway /actuator/health UP"
else
  fail "Gateway /actuator/health UP"
fi

if curl -sf "${GATEWAY_URL}/sim/his/appointments/apt-test-001" | grep -q '"appointmentId":"apt-test-001"'; then
  ok "Gateway /sim/his/appointments/{id} returns 200"
else
  fail "Gateway /sim/his/appointments/{id} returns 200"
fi

if curl -sf "${SIMULATORS_URL}/actuator/health" | grep -q '"status":"UP"'; then
  ok "Simulators /actuator/health UP"
else
  fail "Simulators /actuator/health UP"
fi

if curl -sf "${KEYCLOAK_URL}/realms/${REALM}/.well-known/openid-configuration" | grep -q 'issuer'; then
  ok "Keycloak realm '${REALM}' OIDC discovery"
else
  fail "Keycloak realm '${REALM}' OIDC discovery"
fi

if curl -sf "${PROMETHEUS_URL}/-/healthy" | grep -q 'Healthy'; then
  ok "Prometheus healthy"
else
  fail "Prometheus healthy"
fi

UP_COUNT=$(curl -sf "${PROMETHEUS_URL}/api/v1/targets" | grep -c '"health":"up"' || true)
if [ "${UP_COUNT:-0}" -gt 0 ]; then
  ok "Prometheus has UP targets (${UP_COUNT})"
else
  fail "Prometheus has UP targets"
fi

if docker compose logs kafka-init 2>/dev/null | grep -q 'Kafka topic bootstrap complete'; then
  ok "Kafka topics bootstrapped"
else
  fail "Kafka topics bootstrapped"
fi

if docker compose logs minio-init 2>/dev/null | grep -q 'MinIO bucket bootstrap complete'; then
  ok "MinIO buckets bootstrapped"
else
  fail "MinIO buckets bootstrapped"
fi

echo ""
if [ "$FAILURES" -gt 0 ]; then
  echo "Phase 0 verification FAILED ($FAILURES issue(s))"
  exit 1
fi

echo "Phase 0 verification PASSED"
exit 0
