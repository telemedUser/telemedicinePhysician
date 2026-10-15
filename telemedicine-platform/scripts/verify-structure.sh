#!/usr/bin/env bash
# Structural verification for the telemedicine base project.
# Run from workspace root: bash telemedicine-platform/scripts/verify-structure.sh

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
PASS=0
FAIL=0

check() {
  if [[ -e "$ROOT/$1" ]]; then
    echo "OK  $1"
    PASS=$((PASS + 1))
  else
    echo "MISSING  $1"
    FAIL=$((FAIL + 1))
  fi
}

echo "=== Backend ==="
check "telemedicine-backend/gateway/src/main/resources/application.yml"
check "telemedicine-backend/services/_template-java/Dockerfile"
check "telemedicine-backend/services/_template-python/requirements.txt"
check "telemedicine-backend/shared/python-common/setup.py"
check "telemedicine-backend/deploy/docker-compose.dev.yml"
check "telemedicine-backend/tools/scaffold-service.sh"
check "telemedicine-backend/tools/scaffold-service.ps1"

echo "=== Frontend ==="
check "telemedicine-frontend/apps/shell/vite.config.ts"
check "telemedicine-frontend/apps/_template-mfe/vite.config.ts"
check "telemedicine-frontend/apps/shell/public/config/mfe-manifest.json"
check "telemedicine-frontend/packages/mfe-registry/src/index.ts"
check "telemedicine-frontend/tools/scaffold-mfe.mjs"
check "telemedicine-frontend/nx.json"

echo "=== Platform ==="
check "telemedicine-platform/docker-compose.full-stack.yml"
check "telemedicine-platform/kubernetes/ingress/ingress.yaml"
check "telemedicine-platform/docs/architecture.md"
check "telemedicine-platform/docs/adding-a-service.md"
check "telemedicine-platform/docs/adding-an-mfe.md"
check "telemedicine-platform/ci/templates/backend-service.yml"

echo ""
echo "Passed: $PASS  Failed: $FAIL"
if [[ $FAIL -gt 0 ]]; then
  exit 1
fi

echo "Structure verification passed."
