import { describe, expect, it, beforeAll, afterAll, afterEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { renderWithProviders } from '@telemedicine/test-utils';
import App from './App';

const server = setupServer(
  http.get('/api/bff/admin/config', () =>
    HttpResponse.json({
      data: {
        videoProvider: 'simulator',
        chatProvider: 'simulator',
        telemetryEnabled: false,
        notificationChannels: { email: true, sms: false },
        mfeRemotes: { mfeAdminConfig: true },
        telemetryRulesJson: '{}',
      },
    }),
  ),
  http.put('/api/bff/admin/config/:key', () => HttpResponse.json({ data: { ok: true } })),
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('mfe-admin-config App', () => {
  it('renders platform configuration title', async () => {
    renderWithProviders(<App />, { theme: 'clinical' });
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /platform configuration/i })).toBeInTheDocument();
    });
  });

  it('loads provider selects from admin config', async () => {
    renderWithProviders(<App />, { theme: 'clinical' });
    await waitFor(() => {
      expect(screen.getByDisplayValue('simulator')).toBeInTheDocument();
    });
  });
});
