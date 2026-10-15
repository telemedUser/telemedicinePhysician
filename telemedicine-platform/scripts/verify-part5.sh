#!/usr/bin/env bash
# Part 5 — Reusable Frontend Packages verification
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
FRONTEND="$ROOT/../telemedicine-frontend"
cd "$FRONTEND"

echo "==> 1. pnpm install"
pnpm install

echo "==> 2. Lock file exists"
test -f pnpm-lock.yaml

echo "==> 3. Package tests"
PACKAGES=(
  shared-types design-system api-client auth session-context
  mfe-registry feature-flags video-adapter chat-adapter
  telemetry-adapter consent-kit device-test i18n
  observability-web test-utils
)
for pkg in "${PACKAGES[@]}"; do
  echo "  Testing $pkg..."
  (cd "packages/$pkg" && pnpm exec vitest run)
done

echo "==> 4. Storybook build"
(cd packages/design-system && pnpm exec storybook build --quiet)

echo ""
echo "verify-part5: ALL CHECKS PASSED"
