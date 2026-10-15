import { describe, expect, it } from 'vitest';
import {
  filterRemotesByRole,
  getEnabledRemotes,
  checkVersionCompatibility,
  createRemoteUnavailableFallback,
} from './index';
import type { MfeManifest, MfeRemote } from '@telemedicine/shared-types';

describe('mfe-registry', () => {
  const remotes: MfeRemote[] = [
    { name: 'a', url: 'http://localhost/a.js', route: '/a', enabled: true, roles: ['PATIENT'] },
    { name: 'b', url: 'http://localhost/b.js', route: '/b', enabled: false, roles: ['ADMIN'] },
  ];

  it('filters by role', () => {
    const filtered = filterRemotesByRole(remotes, ['PATIENT']);
    expect(filtered).toHaveLength(1);
    expect(filtered[0].name).toBe('a');
  });

  it('returns enabled remotes', () => {
    expect(getEnabledRemotes({ remotes })).toHaveLength(1);
  });

  it('checks version compatibility', () => {
    const manifest: MfeManifest = { remotes, shellVersion: '1.0.0' };
    const remote: MfeRemote = { ...remotes[0], minShellVersion: '0.9.0' };
    expect(checkVersionCompatibility(manifest, remote).compatible).toBe(true);
  });

  it('creates unavailable fallback', () => {
    expect(createRemoteUnavailableFallback('test').name).toBe('test');
  });
});
