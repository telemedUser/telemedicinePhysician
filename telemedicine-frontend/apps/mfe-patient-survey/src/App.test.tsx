import { beforeEach, describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { renderWithProviders } from '@telemedicine/test-utils';
import App from './App';

const sessionState = {
  sessionId: null,
  appointmentId: 'apt-demo',
  journeyStep: 'survey',
  journeyStatus: 'CONSULTATION_CLOSED',
  appointment: null,
  metadata: {},
};

vi.mock('@telemedicine/api-client', async () => {
  const actual = await vi.importActual<typeof import('@telemedicine/api-client')>('@telemedicine/api-client');
  return {
    ...actual,
    useApiClient: () => ({
      get: vi.fn().mockResolvedValue({
        data: {
          appointmentId: 'apt-demo',
          available: true,
          submitted: false,
          questions: [{ id: 'nps', type: 'nps', prompt: 'How likely are you to recommend us?' }],
        },
      }),
      post: vi.fn().mockResolvedValue({ data: { submitted: true, available: false } }),
    }),
  };
});

describe('mfe-patient-survey App', () => {
  beforeEach(() => {
    sessionStorage.setItem('telemedicine.session', JSON.stringify(sessionState));
  });

  it('renders survey form when journey gate allows access', async () => {
    renderWithProviders(<App />, { route: '/patient/survey' });
    await waitFor(() => {
      expect(screen.getByRole('heading', { level: 2, name: 'Satisfaction survey' })).toBeInTheDocument();
    });
    expect(screen.getByText('How likely are you to recommend us?')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Submit survey' })).toBeInTheDocument();
  });
});
