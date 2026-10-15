import { describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { renderWithProviders } from '@telemedicine/test-utils';
import { App } from './App';

vi.mock('@telemedicine/mfe-registry', () => ({
  fetchMfeManifest: vi.fn().mockResolvedValue({ shellVersion: '1.0.0', remotes: [] }),
  getEnabledRemotes: vi.fn().mockReturnValue([]),
  filterRemotesByRole: vi.fn((r: unknown[]) => r),
  mergeConfigEnablement: vi.fn(async (r: unknown[]) => r),
  loadRemoteModule: vi.fn(),
}));

describe('patient shell App', () => {
  it('renders shell layout title', async () => {
    renderWithProviders(<App />);
    await waitFor(() => {
      expect(screen.getByText('Telemedicine Platform')).toBeInTheDocument();
    });
  });
});
