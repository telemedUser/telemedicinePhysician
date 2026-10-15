#!/usr/bin/env bash
# Part 6 — Microfrontends verification
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
FRONTEND="$ROOT/telemedicine-frontend"
cd "$FRONTEND"

echo "==> 1. pnpm install"
pnpm install

echo "==> 2. MFE and shell tests"
APPS=(
  shell physician-shell admin-shell
  mfe-patient-consent mfe-patient-identity mfe-patient-waiting-room
  mfe-patient-consultation mfe-patient-documents mfe-patient-survey
  mfe-physician-consultation mfe-physician-documentation mfe-physician-patient-context
  mfe-admin-config mfe-admin-dashboards mfe-admin-audit
  mfe-admin-access mfe-admin-monitoring
)
for app in "${APPS[@]}"; do
  if [ -f "apps/$app/vitest.config.ts" ]; then
    echo "  Testing $app..."
    pnpm exec vitest run --config "apps/$app/vitest.config.ts"
  else
    echo "  WARN: No vitest.config.ts for $app"
  fi
done

echo "==> 3. Build all apps"
pnpm nx run-many -t build --projects=apps/*

echo "==> 4. Manifest roles + version"
node -e "
const fs = require('fs');
const paths = [
  'apps/shell/public/config/mfe-manifest.json',
  'apps/physician-shell/public/config/mfe-manifest.json',
  'apps/admin-shell/public/config/mfe-manifest.json',
];
for (const p of paths) {
  const m = JSON.parse(fs.readFileSync(p, 'utf8'));
  if (!m.shellVersion) throw new Error(p + ' missing shellVersion');
  for (const r of m.remotes) {
    if (!r.roles) throw new Error(r.name + ' missing roles in ' + p);
    if (!r.version) throw new Error(r.name + ' missing version in ' + p);
  }
}
"

echo ""
echo "verify-part6: ALL CHECKS PASSED"
