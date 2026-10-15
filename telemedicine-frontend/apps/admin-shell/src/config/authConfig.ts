import type { AuthConfig } from '@telemedicine/auth';

export const authConfig: AuthConfig = {
  keycloakUrl: import.meta.env.VITE_KEYCLOAK_URL ?? 'http://localhost:8180',
  realm: import.meta.env.VITE_KEYCLOAK_REALM ?? 'telemedicine',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? 'admin-console',
  redirectUri: window.location.origin,
  devMode: import.meta.env.VITE_AUTH_MODE === 'dev',
  devRoles: ['ADMIN'],
};
