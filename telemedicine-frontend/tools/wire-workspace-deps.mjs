#!/usr/bin/env node
/**
 * Ensures every app declares workspace:* deps for @telemedicine packages it imports.
 * Run: node tools/wire-workspace-deps.mjs
 */
import { readFileSync, writeFileSync, readdirSync, statSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const appsDir = join(__dirname, '../apps');

const PACKAGE_MAP = {
  '@telemedicine/shared-types': 'workspace:*',
  '@telemedicine/shared-ui': 'workspace:*',
  '@telemedicine/design-system': 'workspace:*',
  '@telemedicine/mfe-registry': 'workspace:*',
  '@telemedicine/api-client': 'workspace:*',
  '@telemedicine/auth': 'workspace:*',
  '@telemedicine/session-context': 'workspace:*',
  '@telemedicine/video-adapter': 'workspace:*',
  '@telemedicine/chat-adapter': 'workspace:*',
  '@telemedicine/telemetry-adapter': 'workspace:*',
  '@telemedicine/consent-kit': 'workspace:*',
  '@telemedicine/device-test': 'workspace:*',
  '@telemedicine/i18n': 'workspace:*',
  '@telemedicine/feature-flags': 'workspace:*',
  '@telemedicine/observability-web': 'workspace:*',
};

function scanImports(dir) {
  const imports = new Set();
  for (const entry of readdirSync(dir)) {
    const full = join(dir, entry);
    if (statSync(full).isDirectory() && entry !== 'node_modules') {
      scanImports(full).forEach((i) => imports.add(i));
    } else if (/\.(tsx?|jsx?)$/.test(entry)) {
      const content = readFileSync(full, 'utf8');
      const matches = content.matchAll(/from ['"]@telemedicine\/[^'"]+['"]/g);
      for (const m of matches) {
        const pkg = m[0].replace(/from ['"]/, '').replace(/['"]/, '');
        imports.add(pkg);
      }
    }
  }
  return imports;
}

for (const app of readdirSync(appsDir)) {
  const appDir = join(appsDir, app);
  if (!statSync(appDir).isDirectory()) continue;
  const pkgPath = join(appDir, 'package.json');
  try {
    const pkg = JSON.parse(readFileSync(pkgPath, 'utf8'));
    pkg.dependencies = pkg.dependencies ?? {};
    const srcDir = join(appDir, 'src');
    const used = scanImports(srcDir);
    let changed = false;
    for (const imp of used) {
      if (PACKAGE_MAP[imp] && pkg.dependencies[imp] !== 'workspace:*') {
        pkg.dependencies[imp] = 'workspace:*';
        changed = true;
      }
    }
    if (changed) {
      writeFileSync(pkgPath, JSON.stringify(pkg, null, 2) + '\n');
      console.log(`Updated ${app}/package.json`);
    }
  } catch {
    /* skip apps without package.json */
  }
}

console.log('Workspace dependency wiring complete.');
