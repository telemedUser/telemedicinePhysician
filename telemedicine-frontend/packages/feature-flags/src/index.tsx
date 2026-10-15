import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import type { ConfigKeys, ConfigSnapshot } from '@telemedicine/shared-types';
import { ConfigKeys as Keys, mfeConfigKey } from '@telemedicine/shared-types';

export interface FeatureFlags {
  [key: string]: boolean | string | number | Record<string, unknown>;
}

export const SAFE_DEFAULTS: FeatureFlags = {
  [Keys.VIDEO_PROVIDER]: 'simulator',
  [Keys.CHAT_PROVIDER]: 'simulator',
  [Keys.TELEMETRY_ENABLED]: false,
  [Keys.NOTIFICATION_EMAIL]: true,
  [Keys.NOTIFICATION_SMS]: true,
  [Keys.NOTIFICATION_WHATSAPP]: true,
  [Keys.NOTIFICATION_PUSH]: true,
};

export interface ConfigServiceClientOptions {
  baseUrl?: string;
  fetchFn?: typeof fetch;
  pollIntervalMs?: number;
}

const FeatureFlagContext = createContext<{
  flags: FeatureFlags;
  loading: boolean;
  error: Error | null;
  client: ConfigServiceClient;
} | null>(null);

export class ConfigServiceClient {
  private readonly baseUrl: string;
  private readonly fetchFn: typeof fetch;
  private cache: FeatureFlags | null = null;
  private cacheExpiry = 0;
  private readonly cacheTtlMs: number;
  private pollTimer: number | null = null;

  constructor(options: ConfigServiceClientOptions = {}) {
    this.baseUrl = (options.baseUrl ?? '/api/v1/config').replace(/\/$/, '');
    this.fetchFn = options.fetchFn ?? fetch;
    this.cacheTtlMs = 60_000;
    if (options.pollIntervalMs) {
      this.startPolling(options.pollIntervalMs);
    }
  }

  async getSnapshot(): Promise<ConfigSnapshot> {
    const flags = await this.getFlags();
    return flags as ConfigSnapshot;
  }

  async getFlags(): Promise<FeatureFlags> {
    if (this.cache && Date.now() < this.cacheExpiry) {
      return this.cache;
    }

    try {
      const res = await this.fetchFn(this.baseUrl);
      if (!res.ok) {
        console.warn('[feature-flags] config-service unreachable, using safe defaults');
        return { ...SAFE_DEFAULTS };
      }
      const data = (await res.json()) as { data?: FeatureFlags } | FeatureFlags;
      const flags = 'data' in data && data.data ? data.data : (data as FeatureFlags);
      this.cache = { ...SAFE_DEFAULTS, ...flags };
      this.cacheExpiry = Date.now() + this.cacheTtlMs;
      try {
        sessionStorage.setItem('telemedicine.config', JSON.stringify(this.cache));
      } catch {
        /* ignore */
      }
      return this.cache;
    } catch {
      console.warn('[feature-flags] config fetch failed, using safe defaults');
      return this.loadSessionFallback();
    }
  }

  async getValue<T extends boolean | string | number>(key: ConfigKeys | string, defaultValue: T): Promise<T> {
    const flags = await this.getFlags();
    const value = flags[key];
    return (value as T) ?? defaultValue;
  }

  async isEnabled(flag: string, defaultValue = false): Promise<boolean> {
    const flags = await this.getFlags();
    const value = flags[flag];
    return typeof value === 'boolean' ? value : defaultValue;
  }

  clearCache(): void {
    this.cache = null;
    this.cacheExpiry = 0;
    sessionStorage.removeItem('telemedicine.config');
  }

  startPolling(intervalMs: number): void {
    this.stopPolling();
    this.pollTimer = window.setInterval(() => {
      this.cache = null;
      void this.getFlags();
    }, intervalMs);
  }

  stopPolling(): void {
    if (this.pollTimer) {
      window.clearInterval(this.pollTimer);
      this.pollTimer = null;
    }
  }

  private loadSessionFallback(): FeatureFlags {
    try {
      const raw = sessionStorage.getItem('telemedicine.config');
      if (raw) return { ...SAFE_DEFAULTS, ...JSON.parse(raw) };
    } catch {
      /* ignore */
    }
    return { ...SAFE_DEFAULTS };
  }
}

export function FeatureFlagProvider({
  children,
  client = new ConfigServiceClient({ pollIntervalMs: 60_000 }),
}: {
  children: ReactNode;
  client?: ConfigServiceClient;
}) {
  const [flags, setFlags] = useState<FeatureFlags>(SAFE_DEFAULTS);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    void client
      .getFlags()
      .then(setFlags)
      .catch((err: Error) => setError(err))
      .finally(() => setLoading(false));
    return () => client.stopPolling();
  }, [client]);

  const value = useMemo(() => ({ flags, loading, error, client }), [flags, loading, error, client]);
  return <FeatureFlagContext.Provider value={value}>{children}</FeatureFlagContext.Provider>;
}

export function useFeatureFlags() {
  const ctx = useContext(FeatureFlagContext);
  if (!ctx) throw new Error('useFeatureFlags must be used within FeatureFlagProvider');
  return ctx;
}

export function useFeatureFlag(key: string, defaultValue = false) {
  const { flags, loading, error } = useFeatureFlags();
  const value = flags[key];
  return {
    enabled: typeof value === 'boolean' ? value : defaultValue,
    loading,
    error,
  };
}

export function useProvider(key: ConfigKeys | string, defaultValue = 'simulator'): string {
  const { flags } = useFeatureFlags();
  const value = flags[key];
  return typeof value === 'string' ? value : defaultValue;
}

export function FeatureGate({
  flag,
  children,
  fallback = null,
}: {
  flag: string;
  children: ReactNode;
  fallback?: ReactNode;
}) {
  const { enabled, loading } = useFeatureFlag(flag);
  if (loading) return null;
  return enabled ? children : fallback;
}

export { Keys as ConfigKeys, mfeConfigKey };
