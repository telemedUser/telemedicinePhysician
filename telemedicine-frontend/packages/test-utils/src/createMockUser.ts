import type { AuthUser, UserRole } from '@telemedicine/auth';

export function createMockUser(overrides: Partial<AuthUser> & { role?: UserRole } = {}): AuthUser {
  const { role = 'PATIENT', ...rest } = overrides;
  return {
    id: 'user-1',
    email: 'patient@example.com',
    roles: [role],
    patientId: 'patient-1',
    ...rest,
  };
}

export const mockAuthConfig = {
  keycloakUrl: 'http://localhost:8080',
  realm: 'telemedicine',
  clientId: 'patient-portal',
  redirectUri: 'http://localhost:5173/callback',
  devMode: true,
};
