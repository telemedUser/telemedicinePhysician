# End-to-end verification script (requires Docker).
# Usage: bash telemedicine-platform/scripts/verify-e2e.sh

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
COMPOSE="docker compose -f $ROOT/telemedicine-platform/docker-compose.full-stack.yml"

echo "Starting full stack..."
$COMPOSE up -d --build

cleanup() {
  echo "Stopping stack..."
  $COMPOSE down
}
trap cleanup EXIT

echo "Waiting for gateway..."
for i in $(seq 1 60); do
  if curl -sf http://localhost:8080/actuator/health >/dev/null 2>&1; then
    break
  fi
  sleep 5
done

echo "Testing Java service via gateway..."
curl -sf http://localhost:8080/api/v1/template-java/hello | grep -q "template-java"

echo "Testing Python service via gateway..."
curl -sf http://localhost:8080/api/v1/template-python/hello | grep -q "template-python"

echo "Testing shell health..."
curl -sf http://localhost:3000/health | grep -q "ok"

echo "Testing MFE health..."
curl -sf http://localhost:3001/health | grep -q "ok"

echo "Testing MFE manifest..."
curl -sf http://localhost:3000/config/mfe-manifest.json | grep -q "templateMfe"

echo "Testing remoteEntry..."
curl -sf http://localhost:3001/assets/remoteEntry.js | grep -q "templateMfe"

echo "Checking Consul for registered services..."
curl -sf "http://localhost:8500/v1/agent/services" | grep -q "template-java"
curl -sf "http://localhost:8500/v1/agent/services" | grep -q "template-python"

echo ""
echo "All E2E checks passed."
