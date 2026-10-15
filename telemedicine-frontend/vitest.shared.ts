import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import { resolve } from 'path';

export function createVitestConfig(packageDir: string) {
  const root = resolve(packageDir, '../..');
  return defineConfig({
    plugins: [react()],
    test: {
      environment: 'jsdom',
      globals: true,
      setupFiles: [resolve(root, 'vitest.setup.ts')],
      coverage: {
        provider: 'v8',
        reporter: ['text', 'json', 'html'],
        thresholds: {
          lines: 80,
          branches: 75,
          functions: 80,
          statements: 80,
        },
      },
    },
    resolve: {
      alias: {
        '@telemedicine/shared-types': resolve(root, 'packages/shared-types/src/index.ts'),
        '@telemedicine/design-system': resolve(root, 'packages/design-system/src/index.tsx'),
        '@telemedicine/shared-ui': resolve(root, 'packages/shared-ui/src/index.tsx'),
        '@telemedicine/api-client': resolve(root, 'packages/api-client/src/index.tsx'),
        '@telemedicine/auth': resolve(root, 'packages/auth/src/index.tsx'),
        '@telemedicine/session-context': resolve(root, 'packages/session-context/src/index.tsx'),
        '@telemedicine/mfe-registry': resolve(root, 'packages/mfe-registry/src/index.ts'),
        '@telemedicine/feature-flags': resolve(root, 'packages/feature-flags/src/index.tsx'),
        '@telemedicine/i18n': resolve(root, 'packages/i18n/src/index.ts'),
        '@telemedicine/observability-web': resolve(root, 'packages/observability-web/src/index.tsx'),
        '@telemedicine/test-utils': resolve(root, 'packages/test-utils/src/index.ts'),
        '@telemedicine/consent-kit': resolve(root, 'packages/consent-kit/src/index.tsx'),
        '@telemedicine/device-test': resolve(root, 'packages/device-test/src/index.ts'),
        '@telemedicine/video-adapter': resolve(root, 'packages/video-adapter/src/index.ts'),
        '@telemedicine/chat-adapter': resolve(root, 'packages/chat-adapter/src/index.ts'),
        '@telemedicine/telemetry-adapter': resolve(root, 'packages/telemetry-adapter/src/index.ts'),
      },
    },
  });
}
