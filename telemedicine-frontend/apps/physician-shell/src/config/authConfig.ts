import type { AuthConfig } from '@telemedicine/auth';

export const authConfig: AuthConfig = {
  keycloakUrl: import.meta.env.VITE_KEYCLOAK_URL ?? 'http://localhost:8180',
  realm: import.meta.env.VITE_KEYCLOAK_REALM ?? 'telemedicine',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? 'physician-ui',
  redirectUri: window.location.origin,
  devMode: import.meta.env.VITE_AUTH_MODE === 'dev',
  devRoles: ['PHYSICIAN'],
};

export const isPipMode = import.meta.env.VITE_PIP_MODE === 'true';
