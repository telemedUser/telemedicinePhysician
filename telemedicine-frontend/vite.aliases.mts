import { resolve } from 'path';

/** Shared Vite aliases matching tsconfig.base.json paths. */
export function telemedicineAliases(fromDir: string): Record<string, string> {
  const packages = resolve(fromDir, '../../packages');
  return {
    '@telemedicine/mfe-registry': resolve(packages, 'mfe-registry/src/index.ts'),
    '@telemedicine/shared-types': resolve(packages, 'shared-types/src/index.ts'),
    '@telemedicine/shared-ui': resolve(packages, 'shared-ui/src/index.tsx'),
    '@telemedicine/api-client': resolve(packages, 'api-client/src/index.tsx'),
    '@telemedicine/auth': resolve(packages, 'auth/src/index.tsx'),
    '@telemedicine/session-context': resolve(packages, 'session-context/src/index.tsx'),
    '@telemedicine/video-adapter': resolve(packages, 'video-adapter/src/index.ts'),
    '@telemedicine/chat-adapter': resolve(packages, 'chat-adapter/src/index.ts'),
    '@telemedicine/telemetry-adapter': resolve(packages, 'telemetry-adapter/src/index.ts'),
    '@telemedicine/consent-kit': resolve(packages, 'consent-kit/src/index.tsx'),
    '@telemedicine/device-test': resolve(packages, 'device-test/src/index.ts'),
    '@telemedicine/i18n': resolve(packages, 'i18n/src/index.ts'),
    '@telemedicine/feature-flags': resolve(packages, 'feature-flags/src/index.tsx'),
    '@telemedicine/observability-web': resolve(packages, 'observability-web/src/index.tsx'),
    '@telemedicine/design-system': resolve(packages, 'design-system/src/index.tsx'),
    '@telemedicine/test-utils': resolve(packages, 'test-utils/src/index.ts'),
  };
}
