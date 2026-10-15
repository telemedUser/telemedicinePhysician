import { describe, expect, it, beforeAll, afterAll, afterEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { renderWithProviders } from '@telemedicine/test-utils';
import App from './App';

const server = setupServer(
  http.get('/api/v1/monitoring/snapshot', () =>
    HttpResponse.json({
      data: {
        activeSessions: 3,
        waitingRoomQueue: 2,
        errorRate: 0.01,
        connectionQuality: 85,
        openAlerts: 1,
        sloBurnRate: 0.4,
      },
    }),
  ),
  http.get('/api/v1/monitoring/sessions', () =>
    HttpResponse.json({
      data: [
        {
          sessionId: 'session-1',
          appointmentId: 'appt-1',
          provider: 'simulator',
          status: 'ACTIVE',
          qualityScore: 90,
        },
      ],
    }),
  ),
  http.get('/api/v1/monitoring/alerts', () =>
    HttpResponse.json({
      data: [
        {
          id: 'alert-1',
          severity: 'warning',
          message: 'Connection quality degraded',
          status: 'OPEN',
          runbookUrl: 'https://example.com/runbook',
        },
      ],
    }),
  ),
  http.post('/api/v1/monitoring/alerts/:id/acknowledge', () =>
    HttpResponse.json({ data: { acknowledged: true } }),
  ),
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('mfe-admin-monitoring App', () => {
  it('renders live monitoring title', async () => {
    renderWithProviders(<App />, { theme: 'clinical' });
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /live monitoring/i })).toBeInTheDocument();
    });
  });

  it('shows snapshot KPI cards', async () => {
    renderWithProviders(<App />, { theme: 'clinical' });
    await waitFor(() => {
      expect(screen.getByText('3')).toBeInTheDocument();
    });
  });

  it('lists live sessions', async () => {
    renderWithProviders(<App />, { theme: 'clinical' });
    await waitFor(() => {
      expect(screen.getByText('session-1')).toBeInTheDocument();
    });
  });
});
