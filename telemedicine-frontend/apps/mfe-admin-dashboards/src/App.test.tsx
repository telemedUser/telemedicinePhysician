import { describe, expect, it, beforeAll, afterAll, afterEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { renderWithProviders } from '@telemedicine/test-utils';
import App from './App';

const server = setupServer(
  http.get('/api/v1/reporting/kpis/:family', ({ params }) =>
    HttpResponse.json({
      data: {
        family: params.family,
        metrics: {
          'telemedicine.api.latency.p95': 120,
          'telemedicine.sessions.active': 4,
        },
      },
    }),
  ),
  http.post('/api/v1/reporting/exports', () =>
    HttpResponse.json({ data: { downloadUrl: 'https://example.com/report.csv' } }),
  ),
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('mfe-admin-dashboards App', () => {
  it('renders KPI dashboards title', async () => {
    renderWithProviders(<App />, { theme: 'clinical', route: '/admin/dashboards/technical' });
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /kpi dashboards/i })).toBeInTheDocument();
    });
  });

  it('shows technical KPI metrics', async () => {
    renderWithProviders(<App />, { theme: 'clinical', route: '/admin/dashboards/technical' });
    await waitFor(() => {
      expect(screen.getByText('telemedicine.api.latency.p95')).toBeInTheDocument();
    });
  });
});
