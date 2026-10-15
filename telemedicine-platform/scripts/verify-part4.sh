#!/usr/bin/env bash
set -euo pipefail

SIMULATOR_URL="${SIMULATOR_URL:-http://localhost:8200}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_ROOT="$SCRIPT_DIR/../../telemedicine-backend"

echo "=== Part 4 verification (external simulators) ==="

if [[ "${SKIP_GRADLE:-}" != "1" ]]; then
  (cd "$BACKEND_ROOT" && ./gradlew :external-simulators:check :java-common-testing:test --tests "*SimulatorStubs*" --no-daemon)
fi

curl -sf "$SIMULATOR_URL/actuator/health" | grep -q '"status":"UP"'
curl -sf "$SIMULATOR_URL/sim/admin/vendors" | grep -q '"his"'

curl -sf -X PUT "$SIMULATOR_URL/sim/admin/vendors/his" \
  -H 'Content-Type: application/json' \
  -d '{"latencyMs":200,"latencyJitterMs":0,"errorRate":0,"timeoutRate":0,"scenario":"success","enabled":true}'

START=$(date +%s%3N)
curl -sf "$SIMULATOR_URL/sim/his/appointments/test-latency-001" >/dev/null
END=$(date +%s%3N)
(( END - START >= 200 )) || { echo "Latency check failed: $((END-START))ms"; exit 1; }

curl -sf -X POST "$SIMULATOR_URL/sim/admin/scenarios/happy" >/dev/null
CHALLENGE=$(curl -sf -X POST "$SIMULATOR_URL/sim/fonasa/2fa/challenge" -H 'Content-Type: application/json' -d '{"patientId":"patient-verify"}')
CHALLENGE_ID=$(echo "$CHALLENGE" | sed -n 's/.*"challengeId":"\([^"]*\)".*/\1/p')
VERIFY=$(curl -sf -X POST "$SIMULATOR_URL/sim/fonasa/2fa/verify" -H 'Content-Type: application/json' -d "{\"patientId\":\"patient-verify\",\"challengeId\":\"$CHALLENGE_ID\",\"code\":\"123456\"}")
echo "$VERIFY" | grep -q '"verified":true'

curl -sf "$SIMULATOR_URL/sim/v3/api-docs" | grep -q '"openapi"'

echo "All Part 4 checks passed."
