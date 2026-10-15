import { beforeEach, describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { renderWithProviders } from '@telemedicine/test-utils';
import App from './App';

vi.mock('@telemedicine/api-client', async () => {
  const actual = await vi.importActual<typeof import('@telemedicine/api-client')>('@telemedicine/api-client');
  return {
    ...actual,
    useApiClient: () => ({
      post: vi.fn().mockResolvedValue({ data: {} }),
    }),
  };
});

describe('mfe-patient-consent App', () => {
  beforeEach(() => {
    sessionStorage.setItem(
      'telemedicine.session',
      JSON.stringify({
        appointmentId: 'apt-demo',
        journeyStatus: 'LINK_GENERATED',
        journeyStep: 'consent',
        sessionId: null,
        appointment: null,
        metadata: {},
      }),
    );
  });

  it('renders platform consent when journey gate allows', async () => {
    renderWithProviders(<App />, { route: '/patient/consent' });
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /Platform Terms/i })).toBeInTheDocument();
    });
  });
});
