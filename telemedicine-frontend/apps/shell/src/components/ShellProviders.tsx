import type { ReactNode } from 'react';
import { AuthProvider, useAuth } from '@telemedicine/auth';
import { ApiClientProvider } from '@telemedicine/api-client';
import { FeatureFlagProvider } from '@telemedicine/feature-flags';
import { I18nProvider } from '@telemedicine/i18n';
import { ObservabilityProvider } from '@telemedicine/observability-web';
import { SessionProvider } from '@telemedicine/session-context';
import { ThemeProvider } from '@telemedicine/design-system';
import { authConfig } from '../config/authConfig';

function ApiClientWithAuth({ children }: { children: ReactNode }) {
  const { getAccessToken } = useAuth();
  return <ApiClientProvider getToken={getAccessToken}>{children}</ApiClientProvider>;
}

export function ShellProviders({ children }: { children: ReactNode }) {
  return (
    <ObservabilityProvider>
      <I18nProvider>
        <AuthProvider config={authConfig}>
          <ApiClientWithAuth>
            <FeatureFlagProvider>
              <ThemeProvider theme="patient">
                <SessionProvider>{children}</SessionProvider>
              </ThemeProvider>
            </FeatureFlagProvider>
          </ApiClientWithAuth>
        </AuthProvider>
      </I18nProvider>
    </ObservabilityProvider>
  );
}
