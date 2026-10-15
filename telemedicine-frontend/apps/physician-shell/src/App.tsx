import { ComponentType, useEffect, useMemo, useState } from 'react';
import { Link, Route, Routes, useSearchParams } from 'react-router-dom';
import {
  fetchMfeManifest,
  filterRemotesByRole,
  getEnabledRemotes,
  loadRemoteModule,
  mergeConfigEnablement,
  createRemoteUnavailableFallback,
} from '@telemedicine/mfe-registry';
import type { MfeRemote } from '@telemedicine/shared-types';
import { ShellLayout, ErrorMessage, LoadingSpinner, EmptyState } from '@telemedicine/design-system';
import { RequireAuth, RequireRole, useAuth } from '@telemedicine/auth';
import { useFeatureFlags } from '@telemedicine/feature-flags';
import { useTranslation } from '@telemedicine/i18n';
import { ShellProviders } from './components/ShellProviders';
import { isPipMode } from './config/authConfig';
import './physicianShell.pip.css';

function RemoteMfePage({ remote }: { remote: MfeRemote }) {
  const [RemoteApp, setRemoteApp] = useState<ComponentType | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadRemoteModule<{ default: ComponentType }>(remote, remote.module ?? './App')
      .then((module) => setRemoteApp(() => module.default))
      .catch((err: Error) => setError(err.message));
  }, [remote]);

  if (error) {
    const fallback = createRemoteUnavailableFallback(remote.name);
    return <EmptyState title={fallback.name} description={`${fallback.message} (${error})`} />;
  }
  if (!RemoteApp) return <LoadingSpinner />;
  return <RemoteApp />;
}

function PipBridge() {
  const [searchParams] = useSearchParams();

  useEffect(() => {
    const handler = (event: MessageEvent) => {
      if (event.data?.type === 'telemedicine:navigate' && typeof event.data.route === 'string') {
        window.location.hash = event.data.route;
      }
    };
    window.addEventListener('message', handler);
    return () => window.removeEventListener('message', handler);
  }, []);

  useEffect(() => {
    const appointmentId = searchParams.get('appointmentId');
    if (appointmentId) {
      window.parent.postMessage(
        { type: 'telemedicine:status', status: 'ready', appointmentId },
        '*',
      );
    }
  }, [searchParams]);

  return null;
}

function ShellApp() {
  const { user } = useAuth();
  const { flags } = useFeatureFlags();
  const { t } = useTranslation('physician');
  const [remotes, setRemotes] = useState<MfeRemote[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchMfeManifest('/config/mfe-manifest.json')
      .then(async (manifest) => {
        let enabled = getEnabledRemotes(manifest);
        enabled = filterRemotesByRole(enabled, user?.roles ?? []);
        enabled = await mergeConfigEnablement(enabled, flags);
        setRemotes(enabled);
      })
      .catch((err: Error) => setError(err.message))
      .finally(() => setLoading(false));
  }, [user?.roles, flags]);

  const remoteRoutes = useMemo(
    () =>
      remotes.map((remote) => (
        <Route
          key={remote.name}
          path={`${remote.route}/*`}
          element={
            <RequireAuth>
              <RequireRole roles={['PHYSICIAN']}>
                <RemoteMfePage remote={remote} />
              </RequireRole>
            </RequireAuth>
          }
        />
      )),
    [remotes],
  );

  if (loading) return <LoadingSpinner />;
  if (error) return <ErrorMessage message={error} />;

  const content = (
    <>
      <PipBridge />
      {!isPipMode && (
        <nav aria-label="Physician navigation" style={{ marginBottom: '1.5rem', display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
          <Link to="/">{t('home')}</Link>
          {remotes.map((remote) => (
            <Link key={remote.name} to={remote.route}>
              {t(remote.route.split('/').pop() ?? remote.name)}
            </Link>
          ))}
        </nav>
      )}
      <Routes>
        <Route
          path="/"
          element={
            isPipMode ? (
              <RemoteMfePage remote={remotes.find((r) => r.route.includes('consultation')) ?? remotes[0]!} />
            ) : (
              <p>{t('shellHint')}</p>
            )
          }
        />
        {remoteRoutes}
      </Routes>
    </>
  );

  if (isPipMode) {
    return <div className="physician-pip">{content}</div>;
  }

  return <ShellLayout title="Physician Portal">{content}</ShellLayout>;
}

export function App() {
  return (
    <ShellProviders>
      <ShellApp />
    </ShellProviders>
  );
}
