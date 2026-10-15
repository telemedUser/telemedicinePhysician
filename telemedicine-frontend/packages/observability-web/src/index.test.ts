import { describe, expect, it, vi } from 'vitest';
import { resetRateLimiter, reportError, reportMetric } from './reporting';

describe('observability-web', () => {
  it('logs to console in dev mode', async () => {
    vi.stubEnv('VITE_OBSERVABILITY_MODE', 'dev');
    const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    await reportError({ message: 'test error' });
    expect(errorSpy).toHaveBeenCalled();
    errorSpy.mockRestore();
    vi.unstubAllEnvs();
  });

  it('rate limits metric reports', async () => {
    resetRateLimiter();
    vi.stubEnv('VITE_OBSERVABILITY_MODE', 'prod');
    const fetchSpy = vi.spyOn(global, 'fetch').mockResolvedValue(new Response('{}'));
    for (let i = 0; i < 12; i++) {
      await reportMetric({ name: 'test', value: i });
    }
    expect(fetchSpy.mock.calls.length).toBeLessThanOrEqual(10);
    fetchSpy.mockRestore();
    resetRateLimiter();
    vi.unstubAllEnvs();
  });
});
