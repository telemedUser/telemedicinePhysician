import { render, type RenderOptions } from '@testing-library/react';
import type { ReactElement, ReactNode } from 'react';
import { MemoryRouter } from 'react-router-dom';
import { ApiClientProvider } from '@telemedicine/api-client';
import { AuthProvider } from '@telemedicine/auth';
import { ThemeProvider } from '@telemedicine/design-system';
import { FeatureFlagProvider } from '@telemedicine/feature-flags';
import { I18nProvider } from '@telemedicine/i18n';
import { ObservabilityProvider } from '@telemedicine/observability-web';
import { SessionProvider } from '@telemedicine/session-context';
import { mockAuthConfig } from './createMockUser';

export interface RenderWithProvidersOptions extends Omit<RenderOptions, 'wrapper'> {
  locale?: 'en' | 'es-CL';
  theme?: 'patient' | 'clinical';
  route?: string;
  withAuth?: boolean;
  withSession?: boolean;
  withI18n?: boolean;
  withFeatureFlags?: boolean;
  withObservability?: boolean;
  withTheme?: boolean;
  withApiClient?: boolean;
  withRouter?: boolean;
}

function AllProviders({
  children,
  options,
}: {
  children: ReactNode;
  options: RenderWithProvidersOptions;
}) {
  const {
    locale = 'en',
    theme = 'patient',
    withAuth = true,
    withSession = true,
    withI18n = true,
    withFeatureFlags = true,
    withObservability = true,
    withTheme = true,
    withApiClient = true,
  } = options;

  let tree = <>{children}</>;

  if (withTheme) tree = <ThemeProvider theme={theme}>{tree}</ThemeProvider>;
  if (withObservability) tree = <ObservabilityProvider>{tree}</ObservabilityProvider>;
  if (withFeatureFlags) tree = <FeatureFlagProvider>{tree}</FeatureFlagProvider>;
  if (withI18n) tree = <I18nProvider locale={locale}>{tree}</I18nProvider>;
  if (withSession) tree = <SessionProvider>{tree}</SessionProvider>;
  if (withApiClient) tree = <ApiClientProvider>{tree}</ApiClientProvider>;
  if (withAuth) tree = <AuthProvider config={mockAuthConfig}>{tree}</AuthProvider>;

  return tree;
}

export function renderWithProviders(ui: ReactElement, options: RenderWithProvidersOptions = {}) {
  const { route = '/', withRouter = true, ...renderOptions } = options;

  const Wrapper = ({ children }: { children: ReactNode }) => {
    const inner = <AllProviders options={options}>{children}</AllProviders>;
    return withRouter ? <MemoryRouter initialEntries={[route]}>{inner}</MemoryRouter> : inner;
  };

  return render(ui, { wrapper: Wrapper, ...renderOptions });
}
