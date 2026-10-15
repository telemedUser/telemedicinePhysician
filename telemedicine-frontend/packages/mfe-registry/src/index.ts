import type { MfeManifest, MfeRemote } from '@telemedicine/shared-types';
import { mfeConfigKey } from '@telemedicine/shared-types';
import type { UserRole } from '@telemedicine/auth';

const DEFAULT_MANIFEST_URL = '/config/mfe-manifest.json';
const CACHE_KEY = 'telemedicine.mfe-manifest';
const CACHE_TTL_MS = 5 * 60 * 1000;

interface CachedManifest {
  manifest: MfeManifest;
  cachedAt: number;
}

export class RemoteLoadError extends Error {
  constructor(
    message: string,
    readonly remote: MfeRemote,
  ) {
    super(message);
    this.name = 'RemoteLoadError';
  }
}

export async function fetchMfeManifest(
  manifestUrl: string = DEFAULT_MANIFEST_URL,
  fetchFn: typeof fetch = fetch,
): Promise<MfeManifest> {
  const cached = readCache();
  if (cached) return cached;

  const response = await fetchFn(manifestUrl);
  if (!response.ok) {
    throw new Error(`Failed to load MFE manifest: ${response.status}`);
  }
  const manifest = (await response.json()) as MfeManifest;
  writeCache(manifest);
  return manifest;
}

export function getEnabledRemotes(manifest: MfeManifest): MfeRemote[] {
  return manifest.remotes.filter((remote) => remote.enabled);
}

export function filterRemotesByRole(remotes: MfeRemote[], userRoles: UserRole[]): MfeRemote[] {
  return remotes.filter((remote) => {
    if (!remote.roles?.length) return true;
    return remote.roles.some((role) => userRoles.includes(role as UserRole));
  });
}

export async function mergeConfigEnablement(
  remotes: MfeRemote[],
  configSnapshot: Record<string, unknown>,
): Promise<MfeRemote[]> {
  return remotes.map((remote) => {
    const key = mfeConfigKey(remote.name);
    const enabled = configSnapshot[key];
    if (typeof enabled === 'boolean') {
      return { ...remote, enabled: remote.enabled && enabled };
    }
    return remote;
  });
}

export function checkVersionCompatibility(
  manifest: MfeManifest,
  remote: MfeRemote,
): { compatible: boolean; warning?: string } {
  if (!remote.minShellVersion || !manifest.shellVersion) {
    return { compatible: true };
  }
  const compatible = manifest.shellVersion >= remote.minShellVersion;
  return {
    compatible,
    warning: compatible
      ? undefined
      : `Remote ${remote.name} requires shell ${remote.minShellVersion}, current ${manifest.shellVersion}`,
  };
}

export async function loadRemoteModule<T = unknown>(
  remote: MfeRemote,
  exposedModule: string = './App',
  options: { retries?: number; retryDelayMs?: number } = {},
): Promise<T> {
  const retries = options.retries ?? 2;
  const retryDelayMs = options.retryDelayMs ?? 1000;
  let lastError: unknown;

  for (let attempt = 0; attempt <= retries; attempt++) {
    try {
      const { init, loadRemote } = await import('@module-federation/runtime');
      init({
        name: 'shell',
        remotes: [{ name: remote.name, entry: remote.url }],
      });
      return (await loadRemote(`${remote.name}/${exposedModule}`)) as T;
    } catch (err) {
      lastError = err;
      if (attempt < retries) {
        await new Promise((r) => setTimeout(r, retryDelayMs));
      }
    }
  }

  window.dispatchEvent(
    new CustomEvent('mfe:loadFailed', { detail: { remote: remote.name, error: lastError } }),
  );
  throw new RemoteLoadError(`Failed to load remote ${remote.name}`, remote);
}

export function createRemoteUnavailableFallback(name: string): { message: string; name: string } {
  return {
    name,
    message: `The ${name} module is temporarily unavailable. Please try refreshing the page.`,
  };
}

function readCache(): MfeManifest | null {
  try {
    const raw = sessionStorage.getItem(CACHE_KEY);
    if (!raw) return null;
    const cached = JSON.parse(raw) as CachedManifest;
    if (Date.now() - cached.cachedAt > CACHE_TTL_MS) {
      sessionStorage.removeItem(CACHE_KEY);
      return null;
    }
    return cached.manifest;
  } catch {
    return null;
  }
}

function writeCache(manifest: MfeManifest): void {
  try {
    const cached: CachedManifest = { manifest, cachedAt: Date.now() };
    sessionStorage.setItem(CACHE_KEY, JSON.stringify(cached));
  } catch {
    /* ignore quota errors */
  }
}

export function clearManifestCache(): void {
  sessionStorage.removeItem(CACHE_KEY);
}
