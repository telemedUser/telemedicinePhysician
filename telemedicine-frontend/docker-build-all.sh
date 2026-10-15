#!/usr/bin/env bash
set -euo pipefail
SRC=/host
DST=/tmp/fe
mkdir -p "${DST}/apps"
cp "${SRC}/package.json" "${SRC}/pnpm-workspace.yaml" "${SRC}/nx.json" "${DST}/"
cp "${SRC}/tsconfig.base.json" "${DST}/" 2>/dev/null || true
cp "${SRC}/vite.aliases.mts" "${DST}/" 2>/dev/null || true
cp "${SRC}/vitest.setup.ts" "${DST}/" 2>/dev/null || true
cp "${SRC}/vitest.shared.ts" "${DST}/" 2>/dev/null || true
cp "${SRC}/vitest.workspace.ts" "${DST}/" 2>/dev/null || true
cp "${SRC}/.eslintrc.cjs" "${DST}/" 2>/dev/null || true
cp "${SRC}/.prettierrc" "${DST}/" 2>/dev/null || true
cp "${SRC}/pnpm-lock.yaml" "${DST}/" 2>/dev/null || true
if [ -d "${SRC}/packages" ]; then
  mkdir -p "${DST}/packages"
  for pkg in "${SRC}/packages"/*; do
    [ -d "$pkg" ] || continue
    name="$(basename "$pkg")"
    mkdir -p "${DST}/packages/${name}"
    tar -C "$pkg" --exclude=node_modules --exclude=dist --exclude=storybook-static -cf - . | tar -C "${DST}/packages/${name}" -xf -
  done
fi
if [ -d "${SRC}/tools" ]; then
  mkdir -p "${DST}/tools"
  tar -C "${SRC}/tools" --exclude=node_modules -cf - . | tar -C "${DST}/tools" -xf -
fi
for app in "${SRC}/apps"/*; do
  [ -d "$app" ] || continue
  name="$(basename "$app")"
  mkdir -p "${DST}/apps/${name}"
  tar -C "$app" --exclude=node_modules --exclude=dist --exclude=.vite -cf - . | tar -C "${DST}/apps/${name}" -xf -
done
cd "${DST}"
npm install -g pnpm@9.15.0
if [ -d /pnpm-store ]; then
  pnpm config set store-dir /pnpm-store
fi
pnpm install
pnpm exec nx run-many -t build --skip-nx-cache
if [ -d dist ]; then
  rm -rf "${SRC}/dist"
  cp -a dist "${SRC}/dist"
fi
echo FRONTEND_BUILD_OK
