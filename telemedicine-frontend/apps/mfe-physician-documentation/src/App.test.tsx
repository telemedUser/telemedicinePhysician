import { describe, expect, it, beforeAll, afterAll, afterEach, vi } from 'vitest';
import { fireEvent, screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { renderWithProviders } from '@telemedicine/test-utils';
import App from './App';

const server = setupServer(
  http.post('/api/bff/physician/documentation/:id', async ({ request }) => {
    const body = await request.json();
    return HttpResponse.json({ data: { accepted: true, payload: body } });
  }),
);

beforeAll(() => server.listen());
afterEach(() => {
  server.resetHandlers();
  sessionStorage.clear();
  vi.useRealTimers();
});
afterAll(() => server.close());

describe('mfe-physician-documentation', () => {
  it('renders documentation tabs with i18n labels', () => {
    renderWithProviders(<App />, {
      route: '/?appointmentId=apt-1',
      theme: 'clinical',
    });
    expect(screen.getByRole('heading', { name: /clinical documentation/i })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /diagnosis/i })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /prescription/i })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /medical leave/i })).toBeInTheDocument();
  });

  it('auto-saves draft to sessionStorage every 30 seconds', async () => {
    vi.useFakeTimers();
    renderWithProviders(<App />, {
      route: '/?appointmentId=apt-1',
      theme: 'clinical',
    });

    fireEvent.change(screen.getByLabelText(/icd-10 code/i), { target: { value: 'J06.9' } });
    vi.advanceTimersByTime(30_000);

    const stored = sessionStorage.getItem('physician-doc-draft-apt-1');
    expect(stored).toContain('J06.9');
  });
});
