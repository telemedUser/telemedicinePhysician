import { ComponentType, useEffect, useState } from 'react';
import type { MfeRemote } from '@telemedicine/shared-types';
import { loadRemoteModule, createRemoteUnavailableFallback } from '@telemedicine/mfe-registry';
import { ErrorMessage, LoadingSpinner, EmptyState } from '@telemedicine/design-system';

interface RemoteAppProps {
  remote: MfeRemote;
}

interface FederatedApp {
  default: ComponentType;
}

export function RemoteMfePage({ remote }: RemoteAppProps) {
  const [RemoteApp, setRemoteApp] = useState<ComponentType | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadRemoteModule<FederatedApp>(remote, remote.module ?? './App')
      .then((module) => setRemoteApp(() => module.default))
      .catch((err: Error) => setError(err.message));
  }, [remote]);

  if (error) {
    const fallback = createRemoteUnavailableFallback(remote.name);
    return (
      <EmptyState title={fallback.name} description={`${fallback.message} (${error})`} />
    );
  }

  if (!RemoteApp) {
    return <LoadingSpinner />;
  }

  return <RemoteApp />;
}
