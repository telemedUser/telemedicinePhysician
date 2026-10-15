# Adding a Microfrontend

## Scaffold

Using Nx generator (after `pnpm install`):

```bash
cd telemedicine-frontend
pnpm nx g @telemedicine/mfe:app patient-mfe --route=/patient --port=3002
```

Or use the fallback script:

```bash
pnpm scaffold:mfe patient-mfe --port 3002 --route /patient
```

This will:
- Copy `apps/_template-mfe` → `apps/patient-mfe`
- Configure Module Federation expose `./App`
- Register the remote in the appropriate shell's `public/config/mfe-manifest.json`

## Manifest entry

Each remote needs an entry in the shell's `mfe-manifest.json`:

```json
{
  "name": "mfePatientConsent",
  "url": "http://localhost:3002/assets/remoteEntry.js",
  "route": "/patient/consent",
  "enabled": true,
  "module": "./App",
  "roles": ["PATIENT"],
  "version": "1.0.0",
  "minShellVersion": "1.0.0"
}
```

In production, point `url` to the deployed remote's `remoteEntry.js`.

## Part 6 conventions

### Provider stack (shell responsibility)

MFEs inherit context from the shell — do **not** duplicate providers inside remotes:

```
ObservabilityProvider → I18nProvider → AuthProvider → ApiClientProvider
  → FeatureFlagProvider → ThemeProvider → SessionProvider
```

### Workspace dependencies

Declare every `@telemedicine/*` import in the MFE's `package.json` as `workspace:*` (not Vite alias only).

### i18n

- No hardcoded user-facing strings in MFE components
- Add keys to `packages/i18n/src/locales/en/index.ts` and `es-CL/index.ts`
- Use `useTranslation('namespace')` then `t('key')`

### Journey gating (patient MFEs)

Use `useJourneyGate(requiredStatus)` from `@telemedicine/session-context` and show `EmptyState` when blocked.

### HTTP

All API calls through `@telemedicine/api-client` — no raw `fetch`/`axios`.

### Tests

Each MFE includes:
- `vitest.config.ts` (extends `vitest.shared.ts`)
- `src/App.test.tsx` using `renderWithProviders` from `@telemedicine/test-utils`

## Development

```bash
pnpm nx serve shell              # patient :3000
pnpm nx serve mfe-patient-consent  # :3002
pnpm nx serve physician-shell    # :3010
pnpm nx serve admin-shell        # :3020
```

The shell dynamically creates routes from the manifest — no shell code changes needed when adding a remote.

## Production deploy

1. Build: `pnpm nx build mfe-patient-consent`
2. Docker: add Dockerfile (copy from `_template-mfe/Dockerfile`)
3. K8s: add Deployment/Service under `deploy/kubernetes/base/`
4. Ingress: add path rule in `telemedicine-platform/kubernetes/ingress/ingress.yaml`

## CORS

MFE nginx configs include `Access-Control-Allow-Origin *` on `/assets/` for Module Federation remote loading.

## Verification

```bash
cd telemedicine-platform
./scripts/verify-part6.ps1
```
