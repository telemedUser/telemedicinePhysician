#!/usr/bin/env node
/**
 * Generates TypeScript types from backend OpenAPI contracts.
 * Run via: pnpm generate:types
 */
import { execSync } from 'child_process';
import { mkdirSync, existsSync } from 'fs';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const contractsDir = resolve(__dirname, '../../../../telemedicine-backend/shared/contracts');
const outDir = resolve(__dirname, '../src/generated');

const contracts = [
  'patient-bff.yaml',
  'physician-bff.yaml',
  'admin-bff.yaml',
  'waiting-room-service.yaml',
  'identity-service.yaml',
  'video-service.yaml',
  'config-service.yaml',
];

if (!existsSync(outDir)) {
  mkdirSync(outDir, { recursive: true });
}

for (const contract of contracts) {
  const input = resolve(contractsDir, contract);
  const output = resolve(outDir, contract.replace('.yaml', '.ts'));
  try {
    execSync(`npx openapi-typescript "${input}" -o "${output}"`, { stdio: 'inherit' });
    console.log(`Generated ${output}`);
  } catch {
    console.warn(`Skipped ${contract} (contract may not exist yet)`);
  }
}

console.log('Type generation complete.');
