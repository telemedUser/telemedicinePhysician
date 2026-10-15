#!/usr/bin/env bash
# Part 7 — Hardening and go-live readiness verification (steps 7.0–7.11)
set -euo pipefail

PLATFORM="$(cd "$(dirname "$0")/.." && pwd)"
REPO="$(cd "$PLATFORM/.." && pwd)"
BACKEND="$REPO/telemedicine-backend"
FRONTEND="$REPO/telemedicine-frontend"
FAILURES=0

pass() { echo -e "\033[32m[ OK ]\033[0m $1"; }
fail() { echo -e "\033[31m[FAIL]\033[0m $1"; FAILURES=$((FAILURES + 1)); }

assert_file() {
  local name="$1" path="$2" pattern="${3:-}"
  if [[ ! -f "$path" ]]; then fail "$name — file not found: $path"; return; fi
  if [[ -n "$pattern" ]] && ! grep -qE "$pattern" "$path"; then fail "$name — pattern not found"; return; fi
  pass "$name"
}

echo "==> Part 7 verification (full checklist)"

# 7.0 Event consumers
assert_file "ConfigChangeListener (video)" "$BACKEND/services/video-service/src/main/java/com/telemedicine/videoservice/kafka/ConfigChangeListener.java" "ConfigurationChanged"
assert_file "AppointmentEventListener" "$BACKEND/services/waiting-room-service/src/main/java/com/telemedicine/waitingroomservice/kafka/AppointmentEventListener.java" "AppointmentCreated"
assert_file "Flyway V4" "$BACKEND/shared/java-common-persistence/src/main/resources/db/migration/common/V4__ensure_consumer_tables.sql" "processed_events"
[[ ! -f "$BACKEND/shared/java-common-persistence/src/main/resources/db/migration/common/V2__processed_events.sql" ]] && pass "Flyway V2 duplicate removed" || fail "Flyway V2 duplicate still exists"

# 7.1 Schemas
SCHEMA_COUNT=$(find "$BACKEND/shared/contracts/events/schemas" -name '*.v1.json' 2>/dev/null | wc -l | tr -d ' ')
[[ "$SCHEMA_COUNT" -ge 28 ]] && pass "7.1 event JSON schemas ($SCHEMA_COUNT)" || fail "7.1 schemas ($SCHEMA_COUNT/28)"

# 7.2 Backend Docker
assert_file "Dockerfile.spring-service" "$BACKEND/docker/Dockerfile.spring-service" "GRADLE_MODULE"

# 7.3 Frontend Docker
assert_file "Dockerfile.mfe" "$FRONTEND/docker/Dockerfile.mfe" "NX_APP"

# 7.4 K8s
assert_file "overlay dev" "$PLATFORM/kubernetes/overlays/dev/kustomization.yaml" "resources:"
assert_file "ingress shells" "$PLATFORM/kubernetes/ingress/ingress.yaml" "physician-shell"

# 7.5 HA/DR
for f in bff-ha.yaml domain-ha.yaml kafka-ha.yaml; do assert_file "HA $f" "$PLATFORM/kubernetes/ha/$f" ""; done
assert_file "DR exercise" "$PLATFORM/kubernetes/dr/dr-exercise.yaml" "CronJob"

# 7.6 CI
for wf in backend-ci.yml frontend-ci.yml deploy-staging.yml deploy-prod.yml performance-test.yml; do
  assert_file "workflow $wf" "$REPO/.github/workflows/$wf" ""
done

# 7.7–7.11 (security, k6, prometheus, docs)
assert_file "security-review.md" "$PLATFORM/docs/security-review.md" "Identity"
assert_file "k6 gateway-load" "$PLATFORM/tests/k6/gateway-load.js" "p\(95\)<300"
assert_file "alert OutboxPublishStalled" "$PLATFORM/infra/prometheus/rules/platform-alerts.yml" "OutboxPublishStalled"
assert_file "production-readiness.md" "$PLATFORM/docs/production-readiness.md" "verify-part7"
assert_file "traceability.json" "$PLATFORM/docs/traceability.json" "NFR"

echo ""
if [[ $FAILURES -gt 0 ]]; then echo -e "\033[31mverify-part7: FAILED ($FAILURES checks)\033[0m"; exit 1; fi
echo -e "\033[32mverify-part7: ALL CHECKS PASSED\033[0m"
exit 0
