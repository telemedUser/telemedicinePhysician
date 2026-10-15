#!/usr/bin/env bash
# Asserts Part 3 exit criteria: domain services, BFF aggregation, journey smoke.
set -euo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
CONSUL_URL="${CONSUL_URL:-http://localhost:8500}"
SKIP_GRADLE="${SKIP_GRADLE:-false}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_ROOT="$(cd "$SCRIPT_DIR/../../telemedicine-backend" && pwd)"

failures=0

assert_ok() {
  local name="$1"
  local condition="$2"
  local detail="${3:-}"
  if [[ "$condition" == "true" ]]; then
    echo "[ OK ] $name"
  else
    echo "[FAIL] $name${detail:+ — $detail}"
    failures=$((failures + 1))
  fi
}

echo
echo "=== Part 3 verification (domain services) ==="

if [[ "$SKIP_GRADLE" != "true" ]]; then
  echo
  echo "--- Gradle check ---"
  (
    cd "$BACKEND_ROOT"
    ./gradlew :integration-service:check :waiting-room-service:check :identity-service:check \
      :notification-service:check :video-service:check :telemetry-service:check \
      :clinical-docs-service:check :survey-service:check :reporting-service:check \
      :monitoring-service:check :patient-bff:check :physician-bff:check :admin-bff:check \
      :external-simulators:check --no-daemon
  ) && assert_ok "Gradle domain + BFF checks" true || assert_ok "Gradle domain + BFF checks" false
fi

domain_services=(
  waiting-room-service identity-service video-service telemetry-service
  clinical-docs-service notification-service integration-service
  survey-service reporting-service monitoring-service
)

echo
echo "--- Compose / Consul ---"
for svc in "${domain_services[@]}"; do
  status="$(docker compose ps "$svc" --format '{{.Status}}' 2>/dev/null || true)"
  if [[ "$status" =~ healthy|Up ]]; then
    assert_ok "Container $svc running" true
  else
    assert_ok "Container $svc running" false "$status"
  fi
done

if curl -sf "$CONSUL_URL/v1/catalog/services" >/dev/null; then
  catalog="$(curl -sf "$CONSUL_URL/v1/catalog/services")"
  for svc in "${domain_services[@]}"; do
    if echo "$catalog" | grep -q "\"$svc\""; then
      assert_ok "Consul catalog has $svc" true
    else
      assert_ok "Consul catalog has $svc" false
    fi
  done
else
  assert_ok "Consul reachable" false
fi

echo
echo "--- Journey API smoke ---"
APPOINTMENT_ID="apt-verify-part3"

if resp="$(curl -sf -X POST "$GATEWAY_URL/v1/appointments/v2" \
  -H 'Content-Type: application/json' \
  -d '{"patientId":"pat-verify","physicianId":"phy-verify"}')"; then
  APPOINTMENT_ID="$(echo "$resp" | python3 -c "import sys,json; d=json.load(sys.stdin); print((d.get('data') or d).get('id','apt-verify-part3'))" 2>/dev/null || echo apt-verify-part3)"
  assert_ok "Integration create appointment" true
else
  assert_ok "Integration create appointment" false
fi

curl -sf -X POST "$GATEWAY_URL/v1/appointments/v2/$APPOINTMENT_ID/payment/validate" >/dev/null \
  && assert_ok "Payment validate" true || assert_ok "Payment validate" false

curl -sf -X POST "$GATEWAY_URL/api/v1/waiting-room/session-link" \
  -H 'Content-Type: application/json' \
  -d "{\"appointmentId\":\"$APPOINTMENT_ID\",\"channel\":\"email\"}" >/dev/null \
  && assert_ok "Session link generation" true || assert_ok "Session link generation" false

curl -sf "$GATEWAY_URL/api/v1/waiting-room/queue/$APPOINTMENT_ID" >/dev/null \
  && assert_ok "Queue position API" true || assert_ok "Queue position API" false

curl -sf -X POST "$GATEWAY_URL/api/v1/video/sessions" \
  -H 'Content-Type: application/json' \
  -d "{\"appointmentId\":\"$APPOINTMENT_ID\"}" >/dev/null \
  && assert_ok "Video session create" true || assert_ok "Video session create" false

curl -sf -X POST "$GATEWAY_URL/api/v1/telemetry/token" \
  -H 'Content-Type: application/json' \
  -d "{\"appointmentId\":\"$APPOINTMENT_ID\"}" >/dev/null \
  && assert_ok "Telemetry token" true || assert_ok "Telemetry token" false

curl -sf "$GATEWAY_URL/api/v1/monitoring/snapshot" >/dev/null \
  && assert_ok "Monitoring snapshot" true || assert_ok "Monitoring snapshot" false

curl -sf "$GATEWAY_URL/api/v1/reporting/kpis" >/dev/null \
  && assert_ok "Reporting KPIs" true || assert_ok "Reporting KPIs" false

curl -sf "$GATEWAY_URL/bff/patient/journey/$APPOINTMENT_ID" >/dev/null \
  && assert_ok "Patient BFF journey" true || assert_ok "Patient BFF journey" false

curl -sf "$GATEWAY_URL/v3/api-docs" >/dev/null \
  && assert_ok "Gateway OpenAPI docs" true || assert_ok "Gateway OpenAPI docs" false

echo
if [[ "$failures" -gt 0 ]]; then
  echo "Part 3 verification FAILED ($failures checks)"
  exit 1
fi

echo "Part 3 verification PASSED"
exit 0
