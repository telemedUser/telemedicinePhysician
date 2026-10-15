import { ComponentType, useEffect, useMemo, useState } from 'react';
import { Link, Route, Routes, useLocation } from 'react-router-dom';
import {
  fetchMfeManifest,
  filterRemotesByRole,
  getEnabledRemotes,
  loadRemoteModule,
  mergeConfigEnablement,
  createRemoteUnavailableFallback,
} from '@telemedicine/mfe-registry';
import type { MfeRemote } from '@telemedicine/shared-types';
import { ShellLayout, ErrorMessage, LoadingSpinner, EmptyState, Breadcrumbs, KpiCard } from '@telemedicine/design-system';
import { RequireAuth, RequireRole, useAuth } from '@telemedicine/auth';
import { useApiClient } from '@telemedicine/api-client';
import { useFeatureFlags } from '@telemedicine/feature-flags';
import { useTranslation } from '@telemedicine/i18n';
import type { AdminSnapshotResponse } from '@telemedicine/shared-types';
import { ShellProviders } from './components/ShellProviders';

const NAV_ITEMS = [
  { route: '/admin/config', key: 'config' },
  { route: '/admin/dashboards', key: 'dashboards' },
  { route: '/admin/audit', key: 'audit' },
  { route: '/admin/access', key: 'access' },
  { route: '/admin/monitoring', key: 'monitoring' },
];

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

function AdminHeader() {
  const client = useApiClient();
  const { user, logout } = useAuth();
  const { t } = useTranslation('admin');
  const [snapshot, setSnapshot] = useState<AdminSnapshotResponse | null>(null);

  useEffect(() => {
    client
      .get<AdminSnapshotResponse>('/bff/admin/snapshot')
      .then((res) => setSnapshot(res.data ?? null))
      .catch(() => setSnapshot(null));
  }, [client]);

  return (
    <header style={{ marginBottom: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
      <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
        <KpiCard label={t('activeSessions')} value={String(snapshot?.activeSessions ?? '—')} />
        <KpiCard label={t('systemHealth')} value={snapshot?.schedulerHealthy ? 'healthy' : 'degraded'} />
      </div>
      <div>
        <span style={{ marginRight: '1rem' }}>{user?.email}</span>
        <button type="button" onClick={() => void logout()}>
          {t('logout')}
        </button>
      </div>
    </header>
  );
}

function ShellApp() {
  const { user } = useAuth();
  const { flags } = useFeatureFlags();
  const { t } = useTranslation('admin');
  const location = useLocation();
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

  const breadcrumbs = useMemo(() => {
    const labels: Record<string, string> = {
      admin: t('home'),
      config: t('config'),
      dashboards: t('dashboards'),
      audit: t('audit'),
      access: t('access'),
      monitoring: t('monitoring'),
    };
    return location.pathname
      .split('/')
      .filter(Boolean)
      .map((p) => labels[p] ?? p);
  }, [location.pathname, t]);

  const remoteRoutes = useMemo(
    () =>
      remotes.map((remote) => (
        <Route
          key={remote.name}
          path={`${remote.route}/*`}
          element={
            <RequireAuth>
              <RequireRole roles={['ADMIN']}>
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

  return (
    <ShellLayout title="Administrator Portal">
      <AdminHeader />
      <Breadcrumbs items={breadcrumbs} />
      <nav aria-label="Admin navigation" style={{ margin: '1rem 0', display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
        <Link to="/">{t('home')}</Link>
        {NAV_ITEMS.map((item) => (
          <Link key={item.route} to={item.route} aria-current={location.pathname.startsWith(item.route) ? 'page' : undefined}>
            {t(item.key as 'config')}
          </Link>
        ))}
      </nav>
      <Routes>
        <Route path="/" element={<p>{t('shellHint')}</p>} />
        {remoteRoutes}
      </Routes>
    </ShellLayout>
  );
}

export function App() {
  return (
    <ShellProviders>
      <ShellApp />
    </ShellProviders>
  );
}
