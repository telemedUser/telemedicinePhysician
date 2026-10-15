import { describe, expect, it, beforeAll, afterAll, afterEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { renderWithProviders } from '@telemedicine/test-utils';
import App from './App';

const server = setupServer(
  http.get('/api/bff/physician/consultation/:id', () =>
    HttpResponse.json({
      data: {
        appointmentId: 'apt-1',
        status: 'OK',
        video: { status: 'OK', sessionId: 'sess-1' },
        chat: { status: 'OK', threadId: 'thread-1' },
        telemetry: { status: 'OK', enabled: true },
        patientContext: {
          status: 'OK',
          patientId: 'pat-1',
          name: 'Jane Doe',
          source: 'FONASA',
          dateOfBirth: '1985-06-15',
          medications: ['Metformin'],
          allergies: ['Penicillin'],
          clinicalHistory: 'Type 2 diabetes',
        },
      },
    }),
  ),
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('mfe-physician-patient-context', () => {
  it('renders patient summary KPI cards', async () => {
    renderWithProviders(<App />, {
      route: '/?appointmentId=apt-1',
      theme: 'clinical',
    });
    expect(await screen.findByRole('heading', { name: /patient context/i })).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.getByText('Jane Doe')).toBeInTheDocument();
      expect(screen.getByText('FONASA')).toBeInTheDocument();
    });
    expect(document.querySelector('[data-phi="true"]')).toBeTruthy();
  });

  it('shows HIS empty state when patient context is degraded', async () => {
    server.use(
      http.get('/api/bff/physician/consultation/:id', () =>
        HttpResponse.json({
          data: {
            appointmentId: 'apt-1',
            status: 'DEGRADED',
            video: { status: 'DEGRADED' },
            chat: { status: 'DEGRADED' },
            telemetry: { status: 'DEGRADED', enabled: false },
            patientContext: { status: 'DEGRADED' },
          },
        }),
      ),
    );

    renderWithProviders(<App />, {
      route: '/?appointmentId=apt-1',
      theme: 'clinical',
    });

    expect(
      await screen.findByRole('heading', {
        name: /data unavailable from his/i,
      }),
    ).toBeInTheDocument();
  });
});
