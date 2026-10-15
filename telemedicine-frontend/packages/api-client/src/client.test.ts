import { describe, expect, it, vi } from 'vitest';
import { ApiClient, ApiError, createCorrelationInterceptor } from './client';
import { ErrorCode } from '@telemedicine/shared-types';

describe('ApiClient', () => {
  it('adds correlation header', async () => {
    const client = new ApiClient('/api');
    client.addRequestInterceptor(createCorrelationInterceptor(() => 'corr-123'));
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ success: true }), { status: 200, headers: { 'content-type': 'application/json' } }),
    );
    vi.stubGlobal('fetch', fetchMock);
    await client.fetch('/test');
    expect(fetchMock).toHaveBeenCalled();
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect((init.headers as Record<string, string>)['X-Correlation-Id']).toBe('corr-123');
    vi.unstubAllGlobals();
  });

  it('parses problem+json errors', async () => {
    const client = new ApiClient('/api');
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            type: 'https://telemedicine.bupa/problems/TM-CORE-0001',
            title: 'Unavailable',
            status: 503,
            code: ErrorCode.SERVICE_UNAVAILABLE,
          }),
          { status: 503, headers: { 'content-type': 'application/problem+json' } },
        ),
      ),
    );
    await expect(client.json('/fail')).rejects.toBeInstanceOf(ApiError);
    vi.unstubAllGlobals();
  });
});
