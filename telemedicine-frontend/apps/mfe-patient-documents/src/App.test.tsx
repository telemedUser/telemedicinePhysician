import { beforeEach, describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { renderWithProviders } from '@telemedicine/test-utils';
import App from './App';

const sessionState = {
  sessionId: null,
  appointmentId: 'apt-demo',
  journeyStep: 'documents',
  journeyStatus: 'CONSULTATION_CLOSED',
  appointment: null,
  metadata: {},
};

vi.mock('@telemedicine/api-client', async () => {
  const actual = await vi.importActual<typeof import('@telemedicine/api-client')>('@telemedicine/api-client');
  return {
    ...actual,
    useApiClient: () => ({
      get: vi.fn().mockImplementation((path: string) => {
        if (path.includes('/clinical-docs/')) {
          return Promise.resolve({
            data: {
              appointmentId: 'apt-demo',
              downloadUrl: '/api/v1/clinical-docs/download/token-1',
              documents: [
                {
                  documentId: 'doc-1',
                  documentType: 'summary',
                  issuedAt: '2026-01-01T10:00:00Z',
                },
              ],
              prescriptions: [],
              medicalLeaves: [],
            },
          });
        }
        return Promise.resolve({ data: null });
      }),
    }),
  };
});

describe('mfe-patient-documents App', () => {
  beforeEach(() => {
    sessionStorage.setItem('telemedicine.session', JSON.stringify(sessionState));
  });

  it('renders documents list when journey gate allows access', async () => {
    renderWithProviders(<App />, { route: '/patient/documents' });
    await waitFor(() => {
      expect(screen.getByRole('heading', { level: 2, name: 'Documents' })).toBeInTheDocument();
    });
    expect(screen.getByText('Consultation summary')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Download' })).toBeInTheDocument();
  });
});
