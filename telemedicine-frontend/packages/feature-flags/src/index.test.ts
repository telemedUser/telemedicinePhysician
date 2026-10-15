import { describe, expect, it, vi } from 'vitest';
import { ConfigServiceClient, SAFE_DEFAULTS } from './index';

describe('feature-flags', () => {
  it('returns safe defaults when config unreachable', async () => {
    const client = new ConfigServiceClient({
      fetchFn: vi.fn().mockRejectedValue(new Error('network')),
    });
    const flags = await client.getFlags();
    expect(flags['video.provider']).toBe('simulator');
    expect(flags['telemetry.enabled']).toBe(false);
  });
});
