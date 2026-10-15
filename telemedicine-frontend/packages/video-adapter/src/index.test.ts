import { describe, expect, it, vi } from 'vitest';
import { createVideoAdapter } from './createVideoAdapter';
import { SimulatorVideoAdapter } from './SimulatorVideoAdapter';

describe('video-adapter', () => {
  it('creates simulator adapter by default', () => {
    const adapter = createVideoAdapter('simulator');
    expect(adapter).toBeInstanceOf(SimulatorVideoAdapter);
  });

  it('falls back to simulator for unknown provider', () => {
    const warn = vi.spyOn(console, 'warn').mockImplementation(() => {});
    const adapter = createVideoAdapter('unknown' as 'simulator');
    expect(adapter).toBeInstanceOf(SimulatorVideoAdapter);
    warn.mockRestore();
  });

  it('supports full session lifecycle', async () => {
    const adapter = new SimulatorVideoAdapter();
    const session = await adapter.createSession('appt-1');
    expect(session.appointmentId).toBe('appt-1');

    const joined: string[] = [];
    adapter.onParticipantJoined((p) => joined.push(p.id));
    await adapter.joinSession(session.sessionId, 'patient');
    expect(joined).toContain('local');

    adapter.simulateJoin({ id: 'remote-1', displayName: 'Dr. Smith' });
    expect(joined).toContain('remote-1');

    await adapter.muteAudio(true);
    await adapter.leaveSession();
    adapter.destroy();
  });

  it('emits network quality changes', async () => {
    const adapter = new SimulatorVideoAdapter();
    const qualities: number[] = [];
    adapter.onNetworkQualityChanged((q) => qualities.push(q));
    const session = await adapter.createSession('appt-2');
    await adapter.joinSession(session.sessionId, 'physician');
    expect(adapter.getNetworkQuality()).toBeGreaterThanOrEqual(1);
    adapter.destroy();
  });
});
