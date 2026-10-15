import type { AuthConfig } from '@telemedicine/auth';

/**
 * OAuth2 PKCE config for the standalone physician app. In a hosted HIS the physician is already
 * signed in (TM-WLT-004); here the app authenticates directly against Keycloak with the PHYSICIAN
 * role. Set VITE_AUTH_MODE=dev to bypass sign-in during local development.
 */
export const authConfig: AuthConfig = {
  keycloakUrl: import.meta.env.VITE_KEYCLOAK_URL ?? 'http://localhost:8180',
  realm: import.meta.env.VITE_KEYCLOAK_REALM ?? 'telemedicine',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? 'physician-ui',
  redirectUri: window.location.origin,
  devMode: import.meta.env.VITE_AUTH_MODE === 'dev',
  devRoles: ['PHYSICIAN'],
};
