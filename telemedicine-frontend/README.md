# Telemedicine Frontend Monorepo

Nx + pnpm workspace with three portal shells, 15 domain MFEs, and 17 reusable packages.

## Structure

### Apps (18)
- **Shells:** `shell` (:3000 patient), `physician-shell` (:3010), `admin-shell` (:3020)
- **Patient MFEs:** consent, identity, waiting-room, consultation, documents, survey
- **Physician MFEs:** consultation, documentation, patient-context
- **Admin MFEs:** config, dashboards, audit, access, monitoring
- **Template:** `_template-mfe` (:3001)

### Packages (17)

| Package | Purpose |
|---------|---------|
| `shared-types` | OpenAPI-aligned DTOs, ProblemDetail, ErrorCode, journey/event types |
| `design-system` | WCAG 2.1 AA themed component library (patient + clinical themes) |
| `shared-ui` | Backward-compat re-exports from design-system |
| `mfe-registry` | RBAC-aware manifest loading, config-driven enablement, retry/fallback |
| `api-client` | Typed HTTP client with auth, correlation, retry, cancellation |
| `auth` | OAuth2 PKCE (Keycloak), silent refresh, role guards |
| `session-context` | Cross-MFE journey state, BFF hydration, gate logic |
| `video-adapter` | Vonage/ACS/Simulator video abstraction |
| `chat-adapter` | Clinical chat with API persistence |
| `telemetry-adapter` | ANURA embed with consent/config gates |
| `consent-kit` | Platform + clinical consent flows with withdrawal |
| `device-test` | Camera/mic/speaker/bandwidth preflight |
| `i18n` | es-CL + en locale bundles, Intl formatters |
| `feature-flags` | Config-service client with safe defaults |
| `observability-web` | Error boundary, web-vitals, journey timing |
| `test-utils` | renderWithProviders, MSW handlers, fixtures |
| `eslint-config` / `tsconfig-presets` | Shared lint and TS config |

## Quick start

```bash
pnpm install
pnpm dev              # patient shell on :3000
pnpm test             # all library tests
pnpm storybook        # design-system Storybook on :6006
pnpm lint             # ESLint across workspace
```

Register remotes in each shell's `public/config/mfe-manifest.json`.

## API proxy

Dev servers proxy `/api/*` to `http://localhost:8080` (backend gateway).

## Verification

```bash
# From telemedicine-platform/
./scripts/verify-part5.ps1   # Packages
./scripts/verify-part6.ps1   # MFEs + shells
```

## Dev workflow (Part 6)

Run a shell and its remotes in separate terminals:

```bash
pnpm nx serve shell                    # :3000
pnpm nx serve mfe-patient-consent      # :3002
pnpm nx serve mfe-patient-identity     # :3003
# ... other remotes as needed
```

Physician PiP mode: `VITE_PIP_MODE=true pnpm nx serve physician-shell`

Auth dev bypass: `VITE_AUTH_MODE=dev pnpm nx serve shell`

## Scaffold new MFE

```bash
pnpm nx g @telemedicine/mfe:app patient-mfe
```

## Type generation

```bash
pnpm generate:types   # OpenAPI → shared-types/generated/
```
