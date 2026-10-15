import { createContext, useContext, useMemo, type ReactNode } from 'react';
import { ApiClient, createDefaultApiClient } from './client';

const ApiClientContext = createContext<ApiClient | null>(null);

export interface ApiClientProviderProps {
  client?: ApiClient;
  getToken?: () => string | null;
  children: ReactNode;
}

export function ApiClientProvider({ client, getToken, children }: ApiClientProviderProps) {
  const value = useMemo(
    () => client ?? createDefaultApiClient(getToken),
    [client, getToken],
  );
  return <ApiClientContext.Provider value={value}>{children}</ApiClientContext.Provider>;
}

export function useApiClient(): ApiClient {
  const client = useContext(ApiClientContext);
  if (!client) {
    return createDefaultApiClient();
  }
  return client;
}

export * from './client';
