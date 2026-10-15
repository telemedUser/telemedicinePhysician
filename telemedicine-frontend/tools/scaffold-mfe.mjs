#!/usr/bin/env node
/**
 * Fallback MFE scaffolder when Nx generator is unavailable.
 * Usage: node tools/scaffold-mfe.mjs patient-mfe [--port 3002] [--route /patient]
 */
import { cpSync, existsSync, readFileSync, writeFileSync, mkdirSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const root = join(__dirname, '..');

const name = process.argv[2];
if (!name) {
  console.error('Usage: node tools/scaffold-mfe.mjs <name> [--port N] [--route /path]');
  process.exit(1);
}

const portIdx = process.argv.indexOf('--port');
const routeIdx = process.argv.indexOf('--route');
const port = portIdx >= 0 ? Number(process.argv[portIdx + 1]) : 3002;
const route = routeIdx >= 0 ? process.argv[routeIdx + 1] : `/${name}`;
const federationName = name.replace(/-([a-z])/g, (_, c) => c.toUpperCase()).replace(/^./, (c) => c.toLowerCase()) + 'Mfe';
const className = name.split('-').map((p) => p.charAt(0).toUpperCase() + p.slice(1)).join('');

const target = join(root, 'apps', name);
const template = join(root, 'apps', '_template-mfe');

if (existsSync(target)) {
  console.error(`Already exists: ${target}`);
  process.exit(1);
}

cpSync(template, target, { recursive: true });

const replacements = [
  ['template-mfe', name],
  ['templateMfe', federationName],
  ['Template Microfrontend', `${className} Microfrontend`],
  ['Template MFE', `${className} MFE`],
  ['3001', String(port)],
];

for (const file of ['vite.config.mts', 'project.json', 'package.json', 'index.html', 'src/App.tsx']) {
  const filePath = join(target, file);
  let content = readFileSync(filePath, 'utf-8');
  for (const [from, to] of replacements) {
    content = content.split(from).join(to);
  }
  writeFileSync(filePath, content);
}

const manifestPath = join(root, 'apps/shell/public/config/mfe-manifest.json');
const manifest = JSON.parse(readFileSync(manifestPath, 'utf-8'));
manifest.remotes.push({
  name: federationName,
  url: `http://localhost:${port}/assets/remoteEntry.js`,
  route,
  enabled: true,
  module: './App',
});
writeFileSync(manifestPath, JSON.stringify(manifest, null, 2) + '\n');

console.log(`Created MFE at apps/${name}`);
console.log(`Registered in mfe-manifest.json with route ${route} on port ${port}`);
