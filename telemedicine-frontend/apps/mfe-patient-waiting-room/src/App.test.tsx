import { beforeEach, describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { renderWithProviders } from '@telemedicine/test-utils';
import App from './App';

const sessionState = {
  sessionId: null,
  appointmentId: 'apt-demo',
  journeyStep: 'waiting-room',
  journeyStatus: 'IDENTITY_VALIDATED',
  appointment: null,
  metadata: { paymentStatus: 'CONFIRMED' },
};

vi.mock('@telemedicine/api-client', async () => {
  const actual = await vi.importActual<typeof import('@telemedicine/api-client')>('@telemedicine/api-client');
  return {
    ...actual,
    useApiClient: () => ({
      get: vi.fn().mockImplementation((path: string) => {
        if (path.includes('/waiting-room/queue/')) {
          return Promise.resolve({
            data: { appointmentId: 'apt-demo', position: 2, estimatedWaitSec: 180 },
          });
        }
        if (path.includes('/video/appointments/')) {
          return Promise.reject(new Error('no session'));
        }
        return Promise.resolve({ data: null });
      }),
    }),
  };
});

describe('mfe-patient-waiting-room App', () => {
  beforeEach(() => {
    sessionStorage.setItem('telemedicine.session', JSON.stringify(sessionState));
  });

  it('renders waiting room title when journey gate allows access', async () => {
    renderWithProviders(<App />, { route: '/patient/waiting-room' });
    await waitFor(() => {
      expect(screen.getByRole('heading', { level: 2, name: 'Waiting Room' })).toBeInTheDocument();
    });
    expect(screen.getByTestId('device-test-panel')).toBeInTheDocument();
  });
});
