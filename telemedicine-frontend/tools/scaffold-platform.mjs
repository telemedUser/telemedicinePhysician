#!/usr/bin/env node
import { mkdirSync, writeFileSync, existsSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const root = join(__dirname, '..');

function write(relPath, content) {
  const full = join(root, relPath);
  mkdirSync(dirname(full), { recursive: true });
  writeFileSync(full, content, 'utf8');
  return relPath;
}

const created = [];

function w(relPath, content) {
  created.push(write(relPath, content));
}

// --- Package helpers ---
function pkgPackage(name, scopeName, main = './src/index.ts', deps = {}, hasReact = false) {
  const allDeps = { ...deps };
  if (hasReact) allDeps.react = '^18.3.1';
  w(`packages/${name}/package.json`, JSON.stringify({
    name: scopeName,
    version: '0.1.0',
    private: true,
    main,
    types: main,
    ...(Object.keys(allDeps).length ? { dependencies: allDeps } : {}),
  }, null, 2) + '\n');

  w(`packages/${name}/project.json`, JSON.stringify({
    name: scopeName.replace('@telemedicine/', ''),
    $schema: '../../node_modules/nx/schemas/project-schema.json',
    sourceRoot: `packages/${name}/src`,
    projectType: 'library',
    tags: ['scope:shared', `type:${name.includes('eslint') || name.includes('tsconfig') ? 'config' : 'lib'}`],
  }, null, 2) + '\n');
}

// --- Packages ---
pkgPackage('api-client', '@telemedicine/api-client');
w('packages/api-client/src/index.ts', `export interface RequestConfig extends RequestInit {
  correlationId?: string;
}

export type RequestInterceptor = (
  config: RequestConfig,
) => RequestConfig | Promise<RequestConfig>;

export class ApiClient {
  private readonly baseUrl: string;
  private readonly interceptors: { request: RequestInterceptor[] } = { request: [] };

  constructor(baseUrl = '/api') {
    this.baseUrl = baseUrl;
  }

  addRequestInterceptor(interceptor: RequestInterceptor): void {
    this.interceptors.request.push(interceptor);
  }

  async fetch(path: string, init: RequestConfig = {}): Promise<Response> {
    let config: RequestConfig = { ...init, headers: { ...(init.headers as Record<string, string>) } };
    for (const interceptor of this.interceptors.request) {
      config = await interceptor(config);
    }
    return fetch(\`\${this.baseUrl}\${path}\`, config);
  }

  async json<T>(path: string, init?: RequestConfig): Promise<T> {
    const response = await this.fetch(path, init);
    if (!response.ok) {
      throw new Error(\`API error \${response.status}: \${response.statusText}\`);
    }
    return response.json() as Promise<T>;
  }
}

export function createAuthInterceptor(getToken: () => string | null): RequestInterceptor {
  return (config) => {
    const token = getToken();
    if (token) {
      config.headers = { ...(config.headers as Record<string, string>), Authorization: \`Bearer \${token}\` };
    }
    return config;
  };
}

export function createCorrelationInterceptor(getId: () => string): RequestInterceptor {
  return (config) => {
    const id = config.correlationId ?? getId();
    config.headers = { ...(config.headers as Record<string, string>), 'X-Correlation-Id': id };
    return config;
  };
}

export function createDefaultApiClient(getToken?: () => string | null): ApiClient {
  const client = new ApiClient();
  client.addRequestInterceptor(createCorrelationInterceptor(() => crypto.randomUUID()));
  if (getToken) {
    client.addRequestInterceptor(createAuthInterceptor(getToken));
  }
  return client;
}
`);

pkgPackage('auth', '@telemedicine/auth', './src/index.ts', {}, true);
w('packages/auth/src/index.ts', `import type { ReactNode } from 'react';

export type UserRole = 'patient' | 'physician' | 'admin' | 'nurse';

export interface AuthUser {
  id: string;
  email: string;
  roles: UserRole[];
}

export interface PkceChallenge {
  codeVerifier: string;
  codeChallenge: string;
  state: string;
}

/** Placeholder OAuth2 PKCE flow — wire to identity provider in production. */
export async function createPkceChallenge(): Promise<PkceChallenge> {
  const codeVerifier = crypto.randomUUID().replace(/-/g, '') + crypto.randomUUID().replace(/-/g, '');
  const encoder = new TextEncoder();
  const data = encoder.encode(codeVerifier);
  const digest = await crypto.subtle.digest('SHA-256', data);
  const codeChallenge = btoa(String.fromCharCode(...new Uint8Array(digest)))
    .replace(/\\+/g, '-')
    .replace(/\\//g, '_')
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
  const url = new URL('/oauth2/authorize', params.issuer);
  url.searchParams.set('response_type', 'code');
  url.searchParams.set('client_id', params.clientId);
  url.searchParams.set('redirect_uri', params.redirectUri);
  url.searchParams.set('code_challenge', params.challenge.codeChallenge);
  url.searchParams.set('code_challenge_method', 'S256');
  url.searchParams.set('state', params.challenge.state);
  url.searchParams.set('scope', (params.scopes ?? ['openid', 'profile']).join(' '));
  return url.toString();
}

export function hasRole(user: AuthUser | null, role: UserRole): boolean {
  return user?.roles.includes(role) ?? false;
}

export function hasAnyRole(user: AuthUser | null, roles: UserRole[]): boolean {
  return roles.some((role) => hasRole(user, role));
}

export interface RoleGuardProps {
  user: AuthUser | null;
  allowed: UserRole[];
  fallback?: ReactNode;
  children: ReactNode;
}

export function RoleGuard({ user, allowed, fallback = null, children }: RoleGuardProps): ReactNode {
  return hasAnyRole(user, allowed) ? children : fallback;
}
`);

pkgPackage('session-context', '@telemedicine/session-context', './src/index.tsx', {}, true);
w('packages/session-context/src/index.tsx', `import { createContext, useContext, useMemo, useState, type ReactNode } from 'react';

export type JourneyStep =
  | 'consent'
  | 'identity'
  | 'waiting-room'
  | 'consultation'
  | 'documents'
  | 'survey'
  | 'complete';

export interface SessionState {
  sessionId: string | null;
  journeyStep: JourneyStep;
  metadata: Record<string, unknown>;
}

export interface SessionContextValue {
  state: SessionState;
  setJourneyStep: (step: JourneyStep) => void;
  setSessionId: (id: string | null) => void;
  updateMetadata: (patch: Record<string, unknown>) => void;
  reset: () => void;
}

const initialState: SessionState = {
  sessionId: null,
  journeyStep: 'consent',
  metadata: {},
};

const SessionContext = createContext<SessionContextValue | null>(null);

export function SessionProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<SessionState>(initialState);

  const value = useMemo<SessionContextValue>(
    () => ({
      state,
      setJourneyStep: (journeyStep) => setState((s) => ({ ...s, journeyStep })),
      setSessionId: (sessionId) => setState((s) => ({ ...s, sessionId })),
      updateMetadata: (patch) => setState((s) => ({ ...s, metadata: { ...s.metadata, ...patch } })),
      reset: () => setState(initialState),
    }),
    [state],
  );

  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}

export function useSession(): SessionContextValue {
  const ctx = useContext(SessionContext);
  if (!ctx) {
    throw new Error('useSession must be used within SessionProvider');
  }
  return ctx;
}
`);

pkgPackage('video-adapter', '@telemedicine/video-adapter');
w('packages/video-adapter/src/index.ts', `export interface VideoParticipant {
  id: string;
  displayName: string;
}

export interface VideoSession {
  sessionId: string;
  participants: VideoParticipant[];
}

export interface VideoAdapter {
  connect(sessionId: string): Promise<VideoSession>;
  disconnect(): Promise<void>;
  muteAudio(muted: boolean): Promise<void>;
  muteVideo(muted: boolean): Promise<void>;
  onParticipantJoined(callback: (participant: VideoParticipant) => void): () => void;
}

/** Simulator for local development without a real video provider. */
export class SimulatorVideoAdapter implements VideoAdapter {
  private session: VideoSession | null = null;
  private listeners: Array<(p: VideoParticipant) => void> = [];

  async connect(sessionId: string): Promise<VideoSession> {
    this.session = {
      sessionId,
      participants: [{ id: 'local', displayName: 'You (simulated)' }],
    };
    return this.session;
  }

  async disconnect(): Promise<void> {
    this.session = null;
  }

  async muteAudio(_muted: boolean): Promise<void> {
    /* no-op simulator */
  }

  async muteVideo(_muted: boolean): Promise<void> {
    /* no-op simulator */
  }

  onParticipantJoined(callback: (participant: VideoParticipant) => void): () => void {
    this.listeners.push(callback);
    return () => {
      this.listeners = this.listeners.filter((l) => l !== callback);
    };
  }

  /** Test helper — simulate a remote participant joining. */
  simulateJoin(participant: VideoParticipant): void {
    if (this.session) {
      this.session.participants.push(participant);
      this.listeners.forEach((cb) => cb(participant));
    }
  }
}

export function createVideoAdapter(provider: 'simulator' = 'simulator'): VideoAdapter {
  if (provider === 'simulator') {
    return new SimulatorVideoAdapter();
  }
  throw new Error(\`Unknown video provider: \${provider}\`);
}
`);

pkgPackage('chat-adapter', '@telemedicine/chat-adapter');
w('packages/chat-adapter/src/index.ts', `export interface ChatMessage {
  id: string;
  senderId: string;
  senderRole: 'patient' | 'physician' | 'system';
  body: string;
  sentAt: string;
  clinical?: boolean;
}

export interface ChatThread {
  threadId: string;
  consultationId: string;
  messages: ChatMessage[];
}

export interface ClinicalChatAdapter {
  getThread(consultationId: string): Promise<ChatThread>;
  sendMessage(threadId: string, body: string, clinical?: boolean): Promise<ChatMessage>;
  subscribe(threadId: string, onMessage: (msg: ChatMessage) => void): () => void;
}

export class InMemoryClinicalChatAdapter implements ClinicalChatAdapter {
  private threads = new Map<string, ChatThread>();
  private subscribers = new Map<string, Set<(msg: ChatMessage) => void>>();

  async getThread(consultationId: string): Promise<ChatThread> {
    let thread = [...this.threads.values()].find((t) => t.consultationId === consultationId);
    if (!thread) {
      thread = { threadId: crypto.randomUUID(), consultationId, messages: [] };
      this.threads.set(thread.threadId, thread);
    }
    return thread;
  }

  async sendMessage(threadId: string, body: string, clinical = false): Promise<ChatMessage> {
    const thread = this.threads.get(threadId);
    if (!thread) throw new Error(\`Thread not found: \${threadId}\`);
    const message: ChatMessage = {
      id: crypto.randomUUID(),
      senderId: 'local',
      senderRole: 'physician',
      body,
      sentAt: new Date().toISOString(),
      clinical,
    };
    thread.messages.push(message);
    this.subscribers.get(threadId)?.forEach((cb) => cb(message));
    return message;
  }

  subscribe(threadId: string, onMessage: (msg: ChatMessage) => void): () => void {
    if (!this.subscribers.has(threadId)) {
      this.subscribers.set(threadId, new Set());
    }
    this.subscribers.get(threadId)!.add(onMessage);
    return () => this.subscribers.get(threadId)?.delete(onMessage);
  }
}
`);

pkgPackage('telemetry-adapter', '@telemedicine/telemetry-adapter');
w('packages/telemetry-adapter/src/index.ts', `export interface VitalSigns {
  heartRate?: number;
  bloodPressureSystolic?: number;
  bloodPressureDiastolic?: number;
  respiratoryRate?: number;
  spo2?: number;
}

export interface AnuraSessionConfig {
  apiKey: string;
  participantId: string;
}

export interface AnuraTelemetryAdapter {
  startSession(config: AnuraSessionConfig): Promise<{ sessionId: string }>;
  stopSession(sessionId: string): Promise<VitalSigns>;
  onVitalsUpdate(callback: (vitals: VitalSigns) => void): () => void;
}

/** Placeholder ANURA wrapper — replace with vendor SDK integration. */
export class StubAnuraTelemetryAdapter implements AnuraTelemetryAdapter {
  private callbacks: Array<(vitals: VitalSigns) => void> = [];

  async startSession(_config: AnuraSessionConfig): Promise<{ sessionId: string }> {
    return { sessionId: crypto.randomUUID() };
  }

  async stopSession(_sessionId: string): Promise<VitalSigns> {
    return { heartRate: 72, spo2: 98 };
  }

  onVitalsUpdate(callback: (vitals: VitalSigns) => void): () => void {
    this.callbacks.push(callback);
    return () => {
      this.callbacks = this.callbacks.filter((cb) => cb !== callback);
    };
  }

  /** Test helper — push simulated vitals. */
  emit(vitals: VitalSigns): void {
    this.callbacks.forEach((cb) => cb(vitals));
  }
}
`);

pkgPackage('consent-kit', '@telemedicine/consent-kit', './src/index.tsx', { '@telemedicine/shared-ui': 'workspace:*' }, true);
w('packages/consent-kit/src/index.tsx', `import type { ReactNode } from 'react';

export interface ConsentItem {
  id: string;
  title: string;
  body: ReactNode;
  required: boolean;
}

export interface PlatformConsentProps {
  items: ConsentItem[];
  accepted: Record<string, boolean>;
  onChange: (id: string, accepted: boolean) => void;
}

export function PlatformConsent({ items, accepted, onChange }: PlatformConsentProps) {
  return (
    <section aria-label="Platform consent">
      <h2>Platform Terms & Privacy</h2>
      <ul style={{ listStyle: 'none', padding: 0 }}>
        {items.map((item) => (
          <li key={item.id} style={{ marginBottom: '1rem' }}>
            <label>
              <input
                type="checkbox"
                checked={accepted[item.id] ?? false}
                onChange={(e) => onChange(item.id, e.target.checked)}
                required={item.required}
              />{' '}
              <strong>{item.title}</strong>
            </label>
            <div style={{ marginTop: '0.25rem', color: '#4b5563' }}>{item.body}</div>
          </li>
        ))}
      </ul>
    </section>
  );
}

export interface ClinicalConsentProps {
  procedure: string;
  risks: string[];
  accepted: boolean;
  onChange: (accepted: boolean) => void;
}

export function ClinicalConsent({ procedure, risks, accepted, onChange }: ClinicalConsentProps) {
  return (
    <section aria-label="Clinical consent">
      <h2>Clinical Consent: {procedure}</h2>
      <ul>
        {risks.map((risk) => (
          <li key={risk}>{risk}</li>
        ))}
      </ul>
      <label>
        <input type="checkbox" checked={accepted} onChange={(e) => onChange(e.target.checked)} /> I
        understand the risks and consent to this telemedicine consultation.
      </label>
    </section>
  );
}
`);

pkgPackage('device-test', '@telemedicine/device-test', './src/index.ts', {}, false);
w('packages/device-test/src/index.ts', `export interface DeviceTestResult {
  passed: boolean;
  label: string;
  detail?: string;
}

export interface PreflightReport {
  camera: DeviceTestResult;
  microphone: DeviceTestResult;
  network: DeviceTestResult;
  allPassed: boolean;
}

export async function testCamera(): Promise<DeviceTestResult> {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ video: true });
    stream.getTracks().forEach((t) => t.stop());
    return { passed: true, label: 'Camera' };
  } catch (err) {
    return { passed: false, label: 'Camera', detail: (err as Error).message };
  }
}

export async function testMicrophone(): Promise<DeviceTestResult> {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
    stream.getTracks().forEach((t) => t.stop());
    return { passed: true, label: 'Microphone' };
  } catch (err) {
    return { passed: false, label: 'Microphone', detail: (err as Error).message };
  }
}

export async function testNetwork(pingUrl = '/api/health'): Promise<DeviceTestResult> {
  try {
    const start = performance.now();
    const res = await fetch(pingUrl, { method: 'GET' });
    const ms = Math.round(performance.now() - start);
    return res.ok
      ? { passed: true, label: 'Network', detail: \`\${ms}ms\` }
      : { passed: false, label: 'Network', detail: \`HTTP \${res.status}\` };
  } catch (err) {
    return { passed: false, label: 'Network', detail: (err as Error).message };
  }
}

export async function runPreflightChecks(pingUrl?: string): Promise<PreflightReport> {
  const [camera, microphone, network] = await Promise.all([
    testCamera(),
    testMicrophone(),
    testNetwork(pingUrl),
  ]);
  return {
    camera,
    microphone,
    network,
    allPassed: camera.passed && microphone.passed && network.passed,
  };
}
`);

pkgPackage('i18n', '@telemedicine/i18n', './src/index.ts');
w('packages/i18n/src/locales/en.ts', `export const en = {
  common: {
    loading: 'Loading...',
    error: 'Something went wrong',
    continue: 'Continue',
    cancel: 'Cancel',
  },
  patient: {
    consent: 'Consent',
    identity: 'Identity Verification',
    waitingRoom: 'Waiting Room',
    consultation: 'Consultation',
  },
  physician: {
    consultation: 'Consultation',
    documentation: 'Documentation',
    patientContext: 'Patient Context',
  },
  admin: {
    config: 'Configuration',
    dashboards: 'Dashboards',
    audit: 'Audit Log',
  },
} as const;
`);

w('packages/i18n/src/locales/es.ts', `export const es = {
  common: {
    loading: 'Cargando...',
    error: 'Algo salió mal',
    continue: 'Continuar',
    cancel: 'Cancelar',
  },
  patient: {
    consent: 'Consentimiento',
    identity: 'Verificación de identidad',
    waitingRoom: 'Sala de espera',
    consultation: 'Consulta',
  },
  physician: {
    consultation: 'Consulta',
    documentation: 'Documentación',
    patientContext: 'Contexto del paciente',
  },
  admin: {
    config: 'Configuración',
    dashboards: 'Paneles',
    audit: 'Registro de auditoría',
  },
} as const;
`);

w('packages/i18n/src/index.ts', `import { en } from './locales/en';
import { es } from './locales/es';

export type Locale = 'en' | 'es';
export type Messages = typeof en;

const catalogs: Record<Locale, Messages> = { en, es };

let currentLocale: Locale = 'en';

export function setLocale(locale: Locale): void {
  currentLocale = locale;
}

export function getLocale(): Locale {
  return currentLocale;
}

export function t(key: string): string {
  const parts = key.split('.');
  let node: unknown = catalogs[currentLocale];
  for (const part of parts) {
    if (node && typeof node === 'object' && part in (node as object)) {
      node = (node as Record<string, unknown>)[part];
    } else {
      return key;
    }
  }
  return typeof node === 'string' ? node : key;
}

export { en, es };
`);

pkgPackage('feature-flags', '@telemedicine/feature-flags');
w('packages/feature-flags/src/index.ts', `export interface FeatureFlags {
  [key: string]: boolean | string | number;
}

export interface ConfigServiceClientOptions {
  baseUrl?: string;
  fetchFn?: typeof fetch;
}

export class ConfigServiceClient {
  private readonly baseUrl: string;
  private readonly fetchFn: typeof fetch;
  private cache: FeatureFlags | null = null;

  constructor(options: ConfigServiceClientOptions = {}) {
    this.baseUrl = options.baseUrl ?? '/api/v1/config';
    this.fetchFn = options.fetchFn ?? fetch;
  }

  async getFlags(): Promise<FeatureFlags> {
    if (this.cache) return this.cache;
    const res = await this.fetchFn(\`\${this.baseUrl}/feature-flags\`);
    if (!res.ok) {
      throw new Error(\`Failed to load feature flags: \${res.status}\`);
    }
    this.cache = (await res.json()) as FeatureFlags;
    return this.cache;
  }

  async isEnabled(flag: string, defaultValue = false): Promise<boolean> {
    const flags = await this.getFlags();
    const value = flags[flag];
    return typeof value === 'boolean' ? value : defaultValue;
  }

  clearCache(): void {
    this.cache = null;
  }
}
`);

pkgPackage('observability-web', '@telemedicine/observability-web', './src/index.tsx', {}, true);
w('packages/observability-web/src/index.tsx', `import { Component, type ErrorInfo, type ReactNode } from 'react';

export interface ErrorBoundaryProps {
  fallback?: ReactNode | ((error: Error) => ReactNode);
  onError?: (error: Error, info: ErrorInfo) => void;
  children: ReactNode;
}

interface ErrorBoundaryState {
  error: Error | null;
}

export class ObservabilityErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  state: ErrorBoundaryState = { error: null };

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { error };
  }

  componentDidCatch(error: Error, info: ErrorInfo): void {
    this.props.onError?.(error, info);
    if (typeof console !== 'undefined') {
      console.error('[observability-web]', error, info.componentStack);
    }
  }

  render(): ReactNode {
    const { error } = this.state;
    if (error) {
      const { fallback } = this.props;
      if (typeof fallback === 'function') return fallback(error);
      if (fallback) return fallback;
      return (
        <div role="alert" style={{ padding: '1rem', border: '1px solid #fecaca', borderRadius: 8 }}>
          <strong>Something went wrong.</strong>
          <p style={{ color: '#b91c1c' }}>{error.message}</p>
        </div>
      );
    }
    return this.props.children;
  }
}

export { ObservabilityErrorBoundary as ErrorBoundary };
`);

pkgPackage('design-system', '@telemedicine/design-system', './src/index.tsx', { '@telemedicine/shared-ui': 'workspace:*' }, true);
w('packages/design-system/src/Button.tsx', `import type { ButtonHTMLAttributes, ReactNode } from 'react';

export type ButtonVariant = 'primary' | 'secondary' | 'ghost';

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  children: ReactNode;
}

const variantStyles: Record<ButtonVariant, React.CSSProperties> = {
  primary: { background: '#0f766e', color: 'white', border: 'none' },
  secondary: { background: 'white', color: '#0f766e', border: '1px solid #0f766e' },
  ghost: { background: 'transparent', color: '#0f766e', border: 'none' },
};

export function Button({ variant = 'primary', children, style, ...rest }: ButtonProps) {
  return (
    <button
      type="button"
      style={{
        padding: '0.5rem 1rem',
        borderRadius: 6,
        cursor: 'pointer',
        fontWeight: 500,
        ...variantStyles[variant],
        ...style,
      }}
      {...rest}
    >
      {children}
    </button>
  );
}
`);

w('packages/design-system/src/index.tsx', `export { ShellLayout, LoadingSpinner, ErrorMessage } from '@telemedicine/shared-ui';
export { Button, type ButtonProps, type ButtonVariant } from './Button';
`);

pkgPackage('eslint-config', '@telemedicine/eslint-config', './index.js');
w('packages/eslint-config/index.js', `/** @type {import('eslint').Linter.Config} */
module.exports = {
  root: true,
  env: { browser: true, es2022: true, node: true },
  parser: '@typescript-eslint/parser',
  parserOptions: { ecmaVersion: 'latest', sourceType: 'module', ecmaFeatures: { jsx: true } },
  plugins: ['@typescript-eslint'],
  extends: ['eslint:recommended'],
  ignorePatterns: ['dist', 'node_modules'],
  rules: {
    'no-unused-vars': 'off',
    '@typescript-eslint/no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
  },
};
`);

w('packages/eslint-config/project.json', JSON.stringify({
  name: 'eslint-config',
  $schema: '../../node_modules/nx/schemas/project-schema.json',
  sourceRoot: 'packages/eslint-config',
  projectType: 'library',
  tags: ['scope:shared', 'type:config'],
}, null, 2) + '\n');

pkgPackage('tsconfig-presets', '@telemedicine/tsconfig-presets', './index.json');
w('packages/tsconfig-presets/base.json', JSON.stringify({
  compilerOptions: {
    composite: true,
    declarationMap: true,
    emitDeclarationOnly: true,
    importHelpers: true,
    isolatedModules: true,
    lib: ['ES2022', 'DOM', 'DOM.Iterable'],
    module: 'ESNext',
    moduleResolution: 'bundler',
    noEmitOnError: true,
    noFallthroughCasesInSwitch: true,
    noImplicitOverride: true,
    noImplicitReturns: true,
    noUnusedLocals: true,
    skipLibCheck: true,
    strict: true,
    target: 'ES2022',
  },
}, null, 2) + '\n');

w('packages/tsconfig-presets/react.json', JSON.stringify({
  extends: './base.json',
  compilerOptions: {
    jsx: 'react-jsx',
    lib: ['ES2022', 'DOM', 'DOM.Iterable'],
  },
}, null, 2) + '\n');

w('packages/tsconfig-presets/index.json', JSON.stringify({
  base: './base.json',
  react: './react.json',
}, null, 2) + '\n');

w('packages/tsconfig-presets/project.json', JSON.stringify({
  name: 'tsconfig-presets',
  $schema: '../../node_modules/nx/schemas/project-schema.json',
  sourceRoot: 'packages/tsconfig-presets',
  projectType: 'library',
  tags: ['scope:shared', 'type:config'],
}, null, 2) + '\n');

// --- App helpers ---
function toFederationName(dirName) {
  return dirName
    .split('-')
    .map((part, i) => (i === 0 ? part : part.charAt(0).toUpperCase() + part.slice(1)))
    .join('');
}

function mfeApp(dirName, port, title, displayName) {
  const fedName = toFederationName(dirName);
  const nxName = dirName;
  const distName = dirName;

  w(`apps/${dirName}/package.json`, JSON.stringify({
    name: nxName,
    version: '0.1.0',
    private: true,
    dependencies: {
      '@telemedicine/shared-types': 'workspace:*',
      '@telemedicine/shared-ui': 'workspace:*',
      react: '^18.3.1',
      'react-dom': '^18.3.1',
    },
  }, null, 2) + '\n');

  w(`apps/${dirName}/project.json`, JSON.stringify({
    name: nxName,
    $schema: '../../node_modules/nx/schemas/project-schema.json',
    sourceRoot: `apps/${dirName}/src`,
    projectType: 'application',
    tags: ['scope:app', 'type:mfe'],
    targets: {
      build: {
        executor: 'nx:run-commands',
        outputs: [`{workspaceRoot}/dist/apps/${distName}`],
        options: { command: 'vite build', cwd: `apps/${dirName}` },
      },
      serve: {
        executor: 'nx:run-commands',
        options: { command: 'vite', cwd: `apps/${dirName}` },
      },
      preview: {
        executor: 'nx:run-commands',
        options: { command: 'vite preview', cwd: `apps/${dirName}` },
      },
    },
  }, null, 2) + '\n');

  w(`apps/${dirName}/tsconfig.json`, JSON.stringify({
    extends: '../../tsconfig.base.json',
    compilerOptions: {
      outDir: `../../dist/out-tsc/apps/${distName}`,
      types: ['vite/client'],
    },
    include: ['src/**/*.ts', 'src/**/*.tsx'],
    exclude: ['node_modules'],
  }, null, 2) + '\n');

  w(`apps/${dirName}/index.html`, `<!doctype html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>${title}</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
`);

  w(`apps/${dirName}/src/main.tsx`, `import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>
);
`);

  w(`apps/${dirName}/src/App.tsx`, `export default function App() {
  return (
    <div>
      <h2>${displayName}</h2>
      <p>Microfrontend remote loaded via Module Federation.</p>
    </div>
  );
}
`);

  w(`apps/${dirName}/vite.config.mts`, `import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { federation } from '@module-federation/vite';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));

export default defineConfig({
  root: __dirname,
  cacheDir: '../../node_modules/.vite/${dirName}',
  plugins: [
    react(),
    federation({
      name: '${fedName}',
      filename: 'remoteEntry.js',
      exposes: {
        './App': './src/App.tsx',
      },
      shared: {
        react: { singleton: true },
        'react-dom': { singleton: true },
      },
      dts: false,
    }),
  ],
  server: {
    port: ${port},
    strictPort: true,
    origin: 'http://localhost:${port}',
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  preview: {
    port: ${port},
    strictPort: true,
  },
  build: {
    outDir: '../../dist/apps/${distName}',
    emptyOutDir: true,
    target: 'esnext',
  },
  resolve: {
    alias: {
      '@telemedicine/shared-types': resolve(__dirname, '../../packages/shared-types/src/index.ts'),
      '@telemedicine/shared-ui': resolve(__dirname, '../../packages/shared-ui/src/index.tsx'),
    },
  },
});
`);
}

function shellApp(dirName, port, title, tag) {
  w(`apps/${dirName}/package.json`, JSON.stringify({
    name: dirName,
    version: '0.1.0',
    private: true,
    dependencies: {
      '@telemedicine/mfe-registry': 'workspace:*',
      '@telemedicine/shared-types': 'workspace:*',
      '@telemedicine/shared-ui': 'workspace:*',
      react: '^18.3.1',
      'react-dom': '^18.3.1',
      'react-router-dom': '^6.28.0',
    },
  }, null, 2) + '\n');

  w(`apps/${dirName}/project.json`, JSON.stringify({
    name: dirName,
    $schema: '../../node_modules/nx/schemas/project-schema.json',
    sourceRoot: `apps/${dirName}/src`,
    projectType: 'application',
    tags: ['scope:app', tag],
    targets: {
      build: {
        executor: 'nx:run-commands',
        outputs: [`{workspaceRoot}/dist/apps/${dirName}`],
        options: { command: 'vite build', cwd: `apps/${dirName}` },
      },
      serve: {
        executor: 'nx:run-commands',
        options: { command: 'vite', cwd: `apps/${dirName}` },
      },
      preview: {
        executor: 'nx:run-commands',
        options: { command: 'vite preview', cwd: `apps/${dirName}` },
      },
    },
  }, null, 2) + '\n');

  w(`apps/${dirName}/tsconfig.json`, JSON.stringify({
    extends: '../../tsconfig.base.json',
    compilerOptions: {
      outDir: `../../dist/out-tsc/apps/${dirName}`,
      types: ['vite/client'],
    },
    include: ['src/**/*.ts', 'src/**/*.tsx'],
    exclude: ['node_modules'],
  }, null, 2) + '\n');

  w(`apps/${dirName}/index.html`, `<!doctype html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>${title}</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
`);

  w(`apps/${dirName}/src/main.tsx`, `import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { App } from './App';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </StrictMode>
);
`);

  const fedName = toFederationName(dirName);
  w(`apps/${dirName}/vite.config.mts`, `import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { federation } from '@module-federation/vite';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));

export default defineConfig({
  root: __dirname,
  cacheDir: '../../node_modules/.vite/${dirName}',
  plugins: [
    react(),
    federation({
      name: '${fedName}',
      remotes: {},
    }),
  ],
  server: {
    port: ${port},
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  preview: {
    port: ${port},
    strictPort: true,
  },
  build: {
    outDir: '../../dist/apps/${dirName}',
    emptyOutDir: true,
    target: 'esnext',
  },
  resolve: {
    alias: {
      '@telemedicine/mfe-registry': resolve(__dirname, '../../packages/mfe-registry/src/index.ts'),
      '@telemedicine/shared-types': resolve(__dirname, '../../packages/shared-types/src/index.ts'),
      '@telemedicine/shared-ui': resolve(__dirname, '../../packages/shared-ui/src/index.tsx'),
    },
  },
});
`);
}

// Shells
shellApp('physician-shell', 3010, 'Physician Portal', 'type:physician-shell');
w('apps/physician-shell/src/App.tsx', `import { Link, Route, Routes } from 'react-router-dom';
import { ShellLayout } from '@telemedicine/shared-ui';

function Home() {
  return (
    <div>
      <h2>Physician Portal</h2>
      <p>Simplified shell for physician-facing microfrontends.</p>
      <ul>
        <li><Link to="/consultation">Consultation</Link></li>
        <li><Link to="/documentation">Documentation</Link></li>
        <li><Link to="/patient-context">Patient Context</Link></li>
      </ul>
    </div>
  );
}

function Placeholder({ name }: { name: string }) {
  return <p>Load <strong>{name}</strong> remote via Module Federation.</p>;
}

export function App() {
  return (
    <ShellLayout title="Physician Portal">
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/consultation" element={<Placeholder name="mfe-physician-consultation" />} />
        <Route path="/documentation" element={<Placeholder name="mfe-physician-documentation" />} />
        <Route path="/patient-context" element={<Placeholder name="mfe-physician-patient-context" />} />
      </Routes>
    </ShellLayout>
  );
}
`);

shellApp('admin-shell', 3020, 'Admin Portal', 'type:admin-shell');
w('apps/admin-shell/src/App.tsx', `import { Link, Route, Routes } from 'react-router-dom';
import { ShellLayout } from '@telemedicine/shared-ui';

function Home() {
  return (
    <div>
      <h2>Admin Portal</h2>
      <p>Simplified shell for admin-facing microfrontends.</p>
      <ul>
        <li><Link to="/config">Config</Link></li>
        <li><Link to="/dashboards">Dashboards</Link></li>
        <li><Link to="/audit">Audit</Link></li>
        <li><Link to="/access">Access</Link></li>
        <li><Link to="/monitoring">Monitoring</Link></li>
      </ul>
    </div>
  );
}

function Placeholder({ name }: { name: string }) {
  return <p>Load <strong>{name}</strong> remote via Module Federation.</p>;
}

export function App() {
  return (
    <ShellLayout title="Admin Portal">
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/config" element={<Placeholder name="mfe-admin-config" />} />
        <Route path="/dashboards" element={<Placeholder name="mfe-admin-dashboards" />} />
        <Route path="/audit" element={<Placeholder name="mfe-admin-audit" />} />
        <Route path="/access" element={<Placeholder name="mfe-admin-access" />} />
        <Route path="/monitoring" element={<Placeholder name="mfe-admin-monitoring" />} />
      </Routes>
    </ShellLayout>
  );
}
`);

// Patient MFEs
const patientMfes = [
  ['mfe-patient-consent', 3002, 'Patient Consent', '/patient/consent'],
  ['mfe-patient-identity', 3003, 'Patient Identity', '/patient/identity'],
  ['mfe-patient-waiting-room', 3004, 'Patient Waiting Room', '/patient/waiting-room'],
  ['mfe-patient-consultation', 3005, 'Patient Consultation', '/patient/consultation'],
  ['mfe-patient-documents', 3006, 'Patient Documents', '/patient/documents'],
  ['mfe-patient-survey', 3007, 'Patient Survey', '/patient/survey'],
];

for (const [dir, port, title] of patientMfes) {
  mfeApp(dir, port, title, title);
}

// Physician MFEs
const physicianMfes = [
  ['mfe-physician-consultation', 3011, 'Physician Consultation'],
  ['mfe-physician-documentation', 3012, 'Physician Documentation'],
  ['mfe-physician-patient-context', 3013, 'Physician Patient Context'],
];
for (const [dir, port, title] of physicianMfes) {
  mfeApp(dir, port, title, title);
}

// Admin MFEs
const adminMfes = [
  ['mfe-admin-config', 3021, 'Admin Config'],
  ['mfe-admin-dashboards', 3022, 'Admin Dashboards'],
  ['mfe-admin-audit', 3023, 'Admin Audit'],
  ['mfe-admin-access', 3024, 'Admin Access'],
  ['mfe-admin-monitoring', 3025, 'Admin Monitoring'],
];
for (const [dir, port, title] of adminMfes) {
  mfeApp(dir, port, title, title);
}

// MFE manifest for patient shell
const manifestRemotes = patientMfes.map(([dir, port, title, route]) => ({
  name: toFederationName(dir),
  url: `http://localhost:${port}/assets/remoteEntry.js`,
  route,
  enabled: true,
  module: './App',
}));

// Keep template MFE entry + add patient remotes
w('apps/shell/public/config/mfe-manifest.json', JSON.stringify({
  remotes: [
    {
      name: 'templateMfe',
      url: 'http://localhost:3001/assets/remoteEntry.js',
      route: '/template',
      enabled: true,
      module: './App',
    },
    ...manifestRemotes,
  ],
}, null, 2) + '\n');

// Update tsconfig.base.json paths
const paths = {
  '@telemedicine/mfe-registry': ['packages/mfe-registry/src/index.ts'],
  '@telemedicine/shared-types': ['packages/shared-types/src/index.ts'],
  '@telemedicine/shared-ui': ['packages/shared-ui/src/index.ts'],
  '@telemedicine/api-client': ['packages/api-client/src/index.ts'],
  '@telemedicine/auth': ['packages/auth/src/index.ts'],
  '@telemedicine/session-context': ['packages/session-context/src/index.tsx'],
  '@telemedicine/video-adapter': ['packages/video-adapter/src/index.ts'],
  '@telemedicine/chat-adapter': ['packages/chat-adapter/src/index.ts'],
  '@telemedicine/telemetry-adapter': ['packages/telemetry-adapter/src/index.ts'],
  '@telemedicine/consent-kit': ['packages/consent-kit/src/index.tsx'],
  '@telemedicine/device-test': ['packages/device-test/src/index.ts'],
  '@telemedicine/i18n': ['packages/i18n/src/index.ts'],
  '@telemedicine/feature-flags': ['packages/feature-flags/src/index.ts'],
  '@telemedicine/observability-web': ['packages/observability-web/src/index.tsx'],
  '@telemedicine/design-system': ['packages/design-system/src/index.tsx'],
};

const tsconfigBase = {
  compilerOptions: {
    composite: true,
    declarationMap: true,
    emitDeclarationOnly: true,
    importHelpers: true,
    isolatedModules: true,
    lib: ['ES2022', 'DOM', 'DOM.Iterable'],
    module: 'ESNext',
    moduleResolution: 'bundler',
    noEmitOnError: true,
    noFallthroughCasesInSwitch: true,
    noImplicitOverride: true,
    noImplicitReturns: true,
    noUnusedLocals: true,
    skipLibCheck: true,
    strict: true,
    target: 'ES2022',
    jsx: 'react-jsx',
    baseUrl: '.',
    paths,
  },
};

w('tsconfig.base.json', JSON.stringify(tsconfigBase, null, 2) + '\n');

console.log(JSON.stringify({ count: created.length, files: created.sort() }, null, 2));
