import { describe, expect, it, beforeAll, afterAll, afterEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { renderWithProviders } from '@telemedicine/test-utils';
import App from './App';

const server = setupServer(
  http.get('/api/bff/admin/access/roles', () =>
    HttpResponse.json({
      data: {
        roles: ['ADMIN', 'PHYSICIAN', 'PATIENT'],
      },
    }),
  ),
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('mfe-admin-access App', () => {
  it('renders access management title', async () => {
    renderWithProviders(<App />, { theme: 'clinical' });
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /access management/i })).toBeInTheDocument();
    });
  });

  it('lists realm roles read-only', async () => {
    renderWithProviders(<App />, { theme: 'clinical' });
    await waitFor(() => {
      expect(screen.getByText('ADMIN')).toBeInTheDocument();
      expect(screen.getByText('PHYSICIAN')).toBeInTheDocument();
    });
  });

  it('links to Keycloak admin console', async () => {
    renderWithProviders(<App />, { theme: 'clinical' });
    await waitFor(() => {
      expect(screen.getByRole('link', { name: /keycloak admin console/i })).toBeInTheDocument();
    });
  });
});
