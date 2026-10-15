import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { Modal } from '@telemedicine/design-system';

export type UserRole = 'PATIENT' | 'PHYSICIAN' | 'ADMIN' | 'NURSE' | 'SERVICE';

export interface AuthUser {
  id: string;
  email: string;
  roles: UserRole[];
  patientId?: string;
  physicianId?: string;
}

export interface AuthConfig {
  keycloakUrl: string;
  realm: string;
  clientId: string;
  redirectUri: string;
  scopes?: string[];
  devMode?: boolean;
  devRoles?: UserRole[];
}

export interface PkceChallenge {
  codeVerifier: string;
  codeChallenge: string;
  state: string;
}

let accessToken: string | null = null;
let refreshToken: string | null = null;
let tokenExpiry = 0;

export async function createPkceChallenge(): Promise<PkceChallenge> {
  const codeVerifier = crypto.randomUUID().replace(/-/g, '') + crypto.randomUUID().replace(/-/g, '');
  const encoder = new TextEncoder();
  const digest = await crypto.subtle.digest('SHA-256', encoder.encode(codeVerifier));
  const codeChallenge = btoa(String.fromCharCode(...new Uint8Array(digest)))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
  return { codeVerifier, codeChallenge, state: crypto.randomUUID() };
}

export function buildAuthorizationUrl(params: {
  issuer: string;
  clientId: string;
  redirectUri: string;
  challenge: PkceChallenge;
  scopes?: string[];
}): string {
  const url = new URL(`${params.issuer}/protocol/openid-connect/auth`);
  url.searchParams.set('response_type', 'code');
  url.searchParams.set('client_id', params.clientId);
  url.searchParams.set('redirect_uri', params.redirectUri);
  url.searchParams.set('code_challenge', params.challenge.codeChallenge);
  url.searchParams.set('code_challenge_method', 'S256');
  url.searchParams.set('state', params.challenge.state);
  url.searchParams.set('scope', (params.scopes ?? ['openid', 'profile']).join(' '));
  return url.toString();
}

export function getAccessToken(): string | null {
  return accessToken;
}

export function hasRole(user: AuthUser | null, role: UserRole): boolean {
  return user?.roles.includes(role) ?? false;
}

export function hasAnyRole(user: AuthUser | null, roles: UserRole[]): boolean {
  return roles.some((role) => hasRole(user, role));
}

interface AuthContextValue {
  user: AuthUser | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: () => Promise<void>;
  logout: () => Promise<void>;
  getAccessToken: () => string | null;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function parseJwt(token: string): Record<string, unknown> {
  const payload = token.split('.')[1];
  return JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
}

function userFromToken(token: string): AuthUser {
  const claims = parseJwt(token);
  const realmAccess = (claims.realm_access as { roles?: string[] })?.roles ?? [];
  const roles = realmAccess.filter((r): r is UserRole =>
    ['PATIENT', 'PHYSICIAN', 'ADMIN', 'NURSE', 'SERVICE'].includes(r),
  );
  return {
    id: String(claims.sub ?? ''),
    email: String(claims.email ?? ''),
    roles,
    patientId: claims.patient_id ? String(claims.patient_id) : undefined,
    physicianId: claims.physician_id ? String(claims.physician_id) : undefined,
  };
}

function createDevUser(roles: UserRole[] = ['PATIENT']): AuthUser {
  return { id: 'dev-user', email: 'dev@telemedicine.local', roles };
}

export function AuthProvider({
  config,
  children,
}: {
  config: AuthConfig;
  children: ReactNode;
}) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [sessionExpired, setSessionExpired] = useState(false);
  const refreshTimer = useRef<number | null>(null);
  const pkceRef = useRef<PkceChallenge | null>(null);

  const issuer = `${config.keycloakUrl}/realms/${config.realm}`;

  const scheduleRefresh = useCallback(
    (expiresIn: number) => {
      if (refreshTimer.current) window.clearTimeout(refreshTimer.current);
      const bufferMs = 60_000;
      const delay = Math.max(expiresIn * 1000 - bufferMs, 5000);
      refreshTimer.current = window.setTimeout(async () => {
        if (config.devMode) return;
        if (!refreshToken) {
          setSessionExpired(true);
          return;
        }
        try {
          const res = await fetch(`${issuer}/protocol/openid-connect/token`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams({
              grant_type: 'refresh_token',
              client_id: config.clientId,
              refresh_token: refreshToken,
            }),
          });
          if (!res.ok) {
            setSessionExpired(true);
            return;
          }
          const data = (await res.json()) as { access_token: string; refresh_token?: string; expires_in: number };
          accessToken = data.access_token;
          refreshToken = data.refresh_token ?? refreshToken;
          tokenExpiry = Date.now() + data.expires_in * 1000;
          setUser(userFromToken(data.access_token));
          scheduleRefresh(data.expires_in);
        } catch {
          setSessionExpired(true);
        }
      }, delay);
    },
    [config.clientId, config.devMode, issuer],
  );

  const devRoles = config.devRoles ?? ['PATIENT'];

  const login = useCallback(async () => {
    if (config.devMode) {
      accessToken = 'dev-token';
      setUser(createDevUser(devRoles));
      return;
    }
    const challenge = await createPkceChallenge();
    pkceRef.current = challenge;
    sessionStorage.setItem('pkce_verifier', challenge.codeVerifier);
    sessionStorage.setItem('pkce_state', challenge.state);
    window.location.href = buildAuthorizationUrl({
      issuer,
      clientId: config.clientId,
      redirectUri: config.redirectUri,
      challenge,
      scopes: config.scopes,
    });
  }, [config, issuer]);

  const logout = useCallback(async () => {
    accessToken = null;
    refreshToken = null;
    setUser(null);
    if (config.devMode) return;
    const url = new URL(`${issuer}/protocol/openid-connect/logout`);
    url.searchParams.set('client_id', config.clientId);
    url.searchParams.set('post_logout_redirect_uri', config.redirectUri);
    window.location.href = url.toString();
  }, [config, issuer]);

  useEffect(() => {
    const init = async () => {
      if (config.devMode) {
        setUser(createDevUser(devRoles));
        accessToken = 'dev-token';
        setIsLoading(false);
        return;
      }

      const params = new URLSearchParams(window.location.search);
      const code = params.get('code');
      const state = params.get('state');
      const storedState = sessionStorage.getItem('pkce_state');
      const verifier = sessionStorage.getItem('pkce_verifier');

      if (code && state && verifier && state === storedState) {
        const res = await fetch(`${issuer}/protocol/openid-connect/token`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: new URLSearchParams({
            grant_type: 'authorization_code',
            client_id: config.clientId,
            code,
            redirect_uri: config.redirectUri,
            code_verifier: verifier,
          }),
        });
        sessionStorage.removeItem('pkce_verifier');
        sessionStorage.removeItem('pkce_state');
        window.history.replaceState({}, '', window.location.pathname);
        if (res.ok) {
          const data = (await res.json()) as { access_token: string; refresh_token?: string; expires_in: number };
          accessToken = data.access_token;
          refreshToken = data.refresh_token ?? null;
          tokenExpiry = Date.now() + data.expires_in * 1000;
          setUser(userFromToken(data.access_token));
          scheduleRefresh(data.expires_in);
        }
      }
      setIsLoading(false);
    };
    void init();
    return () => {
      if (refreshTimer.current) window.clearTimeout(refreshTimer.current);
    };
  }, [config, issuer, scheduleRefresh]);

  useEffect(() => {
    const handler = () => setSessionExpired(true);
    window.addEventListener('session:expired', handler);
    return () => window.removeEventListener('session:expired', handler);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isAuthenticated: !!user,
      isLoading,
      login,
      logout,
      getAccessToken,
    }),
    [user, isLoading, login, logout],
  );

  return (
    <AuthContext.Provider value={value}>
      {children}
      <Modal
        open={sessionExpired}
        title="Session expired"
        onClose={() => setSessionExpired(false)}
        footer={
          <button type="button" onClick={() => void login()}>
            Sign in again
          </button>
        }
      >
        <p>Your session has expired. Please sign in again to continue.</p>
      </Modal>
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}

export interface RoleGuardProps {
  user: AuthUser | null;
  allowed: UserRole[];
  fallback?: ReactNode;
  children: ReactNode;
}

export function RoleGuard({ user, allowed, fallback = null, children }: RoleGuardProps) {
  return hasAnyRole(user, allowed) ? children : fallback;
}

export function RequireAuth({ children }: { children: ReactNode }) {
  const { isAuthenticated, isLoading, login } = useAuth();
  if (isLoading) return null;
  if (!isAuthenticated) {
    void login();
    return null;
  }
  return children;
}

export function RequireRole({ roles, children }: { roles: UserRole[]; children: ReactNode }) {
  const { user } = useAuth();
  return hasAnyRole(user, roles) ? children : <Navigate to="/forbidden" replace />;
}

export function ProtectedRoute({ roles, children }: { roles?: UserRole[]; children: ReactNode }) {
  const { isAuthenticated, isLoading } = useAuth();
  const location = useLocation();
  if (isLoading) return null;
  if (!isAuthenticated) return <Navigate to="/login" state={{ from: location }} replace />;
  if (roles) return <RequireRole roles={roles}>{children}</RequireRole>;
  return children;
}

export type { AuthConfig as AuthConfigType };
