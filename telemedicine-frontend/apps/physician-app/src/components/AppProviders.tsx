import type { ReactNode } from 'react';
import { AuthProvider, useAuth } from '@telemedicine/auth';
import { ApiClientProvider } from '@telemedicine/api-client';
import { FeatureFlagProvider } from '@telemedicine/feature-flags';
import { I18nProvider } from '@telemedicine/i18n';
import { ObservabilityProvider } from '@telemedicine/observability-web';
import { ThemeProvider } from '@telemedicine/design-system';
import { authConfig } from '../config/authConfig';

function ApiClientWithAuth({ children }: { children: ReactNode }) {
  const { getAccessToken } = useAuth();
  return <ApiClientProvider getToken={getAccessToken}>{children}</ApiClientProvider>;
}

/**
 * Provider composition for the standalone physician app. Mirrors the shell's provider stack but
 * omits the MFE registry and cross-MFE session context — this app has no remotes to orchestrate.
 */
export function AppProviders({ children }: { children: ReactNode }) {
  return (
    <ObservabilityProvider>
      <I18nProvider>
        <AuthProvider config={authConfig}>
          <ApiClientWithAuth>
            <FeatureFlagProvider>
              <ThemeProvider theme="clinical">{children}</ThemeProvider>
            </FeatureFlagProvider>
          </ApiClientWithAuth>
        </AuthProvider>
      </I18nProvider>
    </ObservabilityProvider>
  );
}
