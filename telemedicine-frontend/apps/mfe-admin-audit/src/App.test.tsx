import { describe, expect, it, beforeAll, afterAll, afterEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { renderWithProviders } from '@telemedicine/test-utils';
import App from './App';

const server = setupServer(
  http.get('/api/bff/admin/audit', () =>
    HttpResponse.json({
      data: {
        events: [
          {
            auditId: 'audit-1',
            actorId: 'admin-1',
            actorRole: 'ADMIN',
            action: 'CONFIG_UPDATED',
            resourceType: 'CONFIG',
            resourceId: 'video.provider',
            outcome: 'SUCCESS',
            occurredAt: '2026-01-01T10:00:00Z',
            hash: 'abc123',
          },
        ],
      },
    }),
  ),
  http.get('/api/v1/audit/consent', () =>
    HttpResponse.json({
      data: [
        {
          consentId: 'consent-1',
          appointmentId: 'appt-1',
          consentType: 'platform',
          granted: true,
          recordedAt: '2026-01-01T10:00:00Z',
        },
      ],
    }),
  ),
  http.post('/api/v1/audit/exports', () =>
    HttpResponse.json({ data: { downloadUrl: 'https://example.com/audit.csv' } }),
  ),
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('mfe-admin-audit App', () => {
  it('renders audit log title', async () => {
    renderWithProviders(<App />, { theme: 'clinical' });
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /audit log/i })).toBeInTheDocument();
    });
  });

  it('loads audit events from BFF', async () => {
    renderWithProviders(<App />, { theme: 'clinical' });
    await waitFor(() => {
      expect(screen.getByText('CONFIG_UPDATED')).toBeInTheDocument();
    });
  });
});
