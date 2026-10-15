import { useEffect, useMemo, useState } from 'react';
import { Route, Routes } from 'react-router-dom';
import {
  fetchMfeManifest,
  filterRemotesByRole,
  getEnabledRemotes,
  mergeConfigEnablement,
} from '@telemedicine/mfe-registry';
import type { MfeRemote } from '@telemedicine/shared-types';
import { ShellLayout, ErrorMessage, LoadingSpinner } from '@telemedicine/design-system';
import { RequireAuth, useAuth } from '@telemedicine/auth';
import { useSession } from '@telemedicine/session-context';
import { useFeatureFlags } from '@telemedicine/feature-flags';
import { ShellProviders } from './components/ShellProviders';
import { PatientJourneyNav } from './components/PatientJourneyNav';
import { HomePage } from './pages/HomePage';
import { RemoteMfePage } from './pages/RemoteMfePage';

function ShellApp() {
  const { user } = useAuth();
  const { flags } = useFeatureFlags();
  const { hydrateFromBff, state } = useSession();
  const [remotes, setRemotes] = useState<MfeRemote[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchMfeManifest()
      .then(async (manifest) => {
        let enabled = getEnabledRemotes(manifest);
        enabled = filterRemotesByRole(enabled, user?.roles ?? []);
        enabled = await mergeConfigEnablement(enabled, flags);
        setRemotes(enabled);
      })
      .catch((err: Error) => setError(err.message))
      .finally(() => setLoading(false));
  }, [user?.roles, flags]);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const appointmentId = params.get('appointmentId') ?? state.appointmentId ?? 'apt-demo';
    if (appointmentId) {
      void hydrateFromBff(appointmentId).catch(() => undefined);
    }
  }, [hydrateFromBff, state.appointmentId]);

  const remoteRoutes = useMemo(
    () =>
      remotes.map((remote) => (
        <Route
          key={remote.name}
          path={`${remote.route}/*`}
          element={
            <RequireAuth>
              <RemoteMfePage remote={remote} />
            </RequireAuth>
          }
        />
      )),
    [remotes],
  );

  if (loading) return <LoadingSpinner />;
  if (error) return <ErrorMessage message={error} />;

  return (
    <ShellLayout title="Telemedicine Platform">
      <PatientJourneyNav remotes={remotes} />
      <Routes>
        <Route path="/" element={<HomePage />} />
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
