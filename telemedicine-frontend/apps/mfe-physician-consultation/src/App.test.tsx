import { describe, expect, it, beforeAll, afterAll, afterEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { renderWithProviders } from '@telemedicine/test-utils';
import App from './App';

const server = setupServer(
  http.get('/api/bff/physician/worklist', () =>
    HttpResponse.json({
      data: {
        status: 'OK',
        entries: [
          {
            appointmentId: 'apt-1',
            status: 'CHECKED_IN',
            patientId: 'pat-1',
          },
        ],
      },
    }),
  ),
  http.put('/api/v1/waiting-room/status/:id', () =>
    HttpResponse.json({ data: { status: 'OPEN_DOCTOR' } }),
  ),
  http.get('/api/bff/physician/consultation/:id', () =>
    HttpResponse.json({
      data: {
        appointmentId: 'apt-1',
        status: 'OK',
        video: { status: 'OK', sessionId: 'sess-1', token: 'tok-1' },
        chat: { status: 'OK', threadId: 'thread-1' },
        telemetry: { status: 'OK', enabled: true },
        patientContext: { status: 'OK', patientId: 'pat-1', name: 'Jane Doe', source: 'sim-his' },
      },
    }),
  ),
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('mfe-physician-consultation', () => {
  it('renders worklist from BFF', async () => {
    renderWithProviders(<App />, { route: '/', theme: 'clinical' });
    expect(await screen.findByRole('heading', { name: /consultation worklist/i })).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.getByText('apt-1')).toBeInTheDocument();
    });
  });

  it('shows admit action for checked-in patients', async () => {
    renderWithProviders(<App />, { route: '/', theme: 'clinical' });
    expect(await screen.findByRole('button', { name: /admit patient/i })).toBeEnabled();
  });
});
