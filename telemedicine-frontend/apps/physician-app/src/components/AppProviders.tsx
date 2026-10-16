import { useMemo, type ReactNode } from 'react';
import { AuthProvider, useAuth } from '@telemedicine/auth';
import { ApiClientProvider, createDefaultApiClient } from '@telemedicine/api-client';
import { FeatureFlagProvider } from '@telemedicine/feature-flags';
import { I18nProvider } from '@telemedicine/i18n';
import { ObservabilityProvider } from '@telemedicine/observability-web';
import { ThemeProvider } from '@telemedicine/design-system';
import { authConfig } from '../config/authConfig';
import { currentPhysicianId } from '../auth/physicianSession';

function ApiClientWithAuth({ children }: { children: ReactNode }) {
  const { getAccessToken } = useAuth();
  // Build the client once and attach an interceptor that stamps the signed-in physician's id on
  // every request (X-Physician-Id). The physician-service dev chain uses it to scope the worklist.
  const client = useMemo(() => {
    const c = createDefaultApiClient(getAccessToken);
    c.addRequestInterceptor((config) => {
      const id = currentPhysicianId();
      if (id) {
        config.headers = {
          ...(config.headers as Record<string, string>),
          'X-Physician-Id': id,
        };
      }
      return config;
    });
    return c;
  }, [getAccessToken]);
  return <ApiClientProvider client={client}>{children}</ApiClientProvider>;
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
