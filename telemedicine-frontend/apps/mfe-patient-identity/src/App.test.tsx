import { beforeEach, describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { renderWithProviders } from '@telemedicine/test-utils';
import App from './App';

vi.mock('@telemedicine/api-client', async () => {
  const actual = await vi.importActual<typeof import('@telemedicine/api-client')>('@telemedicine/api-client');
  return {
    ...actual,
    useApiClient: () => ({
      post: vi.fn().mockResolvedValue({ data: { challengeId: 'ch-1' } }),
    }),
  };
});

describe('mfe-patient-identity App', () => {
  beforeEach(() => {
    sessionStorage.setItem(
      'telemedicine.session',
      JSON.stringify({
        appointmentId: 'apt-demo',
        journeyStatus: 'CONSENT_GRANTED',
        journeyStep: 'identity',
        sessionId: null,
        appointment: null,
        metadata: {},
      }),
    );
  });

  it('renders identity form when journey gate allows', async () => {
    renderWithProviders(<App />, { route: '/patient/identity' });
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Identity Verification' })).toBeInTheDocument();
    });
  });
});
