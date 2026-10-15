import { describe, expect, it } from 'vitest';
import { createPkceChallenge, hasRole, hasAnyRole } from './index';

describe('auth', () => {
  it('creates PKCE challenge', async () => {
    const challenge = await createPkceChallenge();
    expect(challenge.codeVerifier).toBeTruthy();
    expect(challenge.codeChallenge).toBeTruthy();
    expect(challenge.state).toBeTruthy();
  });

  it('checks roles', () => {
    const user = { id: '1', email: 'a@b.com', roles: ['PATIENT' as const] };
    expect(hasRole(user, 'PATIENT')).toBe(true);
    expect(hasAnyRole(user, ['ADMIN'])).toBe(false);
  });
});
