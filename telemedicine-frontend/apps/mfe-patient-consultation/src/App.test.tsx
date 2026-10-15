import type { ReactNode } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import { renderWithProviders } from '@telemedicine/test-utils';
import App from './App';

const sessionState = {
  sessionId: null,
  appointmentId: 'apt-demo',
  journeyStep: 'consultation',
  journeyStatus: 'OPEN_DOCTOR',
  appointment: null,
  metadata: { clinicalConsentGranted: true },
};

vi.mock('@telemedicine/video-adapter', () => ({
  useVideoSession: () => ({
    sessionId: 'session-1',
    isConnected: true,
    isMuted: { audio: false, video: false },
    networkQuality: 4,
    participants: [],
    localStream: null,
    remoteStreams: [],
    toggleAudio: vi.fn(),
    toggleVideo: vi.fn(),
    switchDevice: vi.fn(),
    join: vi.fn(),
    leave: vi.fn().mockResolvedValue(undefined),
    error: null,
  }),
}));

vi.mock('@telemedicine/chat-adapter', () => ({
  useChat: () => ({
    messages: [],
    typing: false,
    connected: true,
    error: null,
    connect: vi.fn(),
    sendMessage: vi.fn(),
    setTyping: vi.fn(),
    disconnect: vi.fn().mockResolvedValue(undefined),
  }),
}));

vi.mock('@telemedicine/telemetry-adapter', () => ({
  TelemetryConsentGate: ({ children }: { children: ReactNode }) => <>{children}</>,
  TelemetryConfigGate: ({ children }: { children: ReactNode }) => <>{children}</>,
  TelemetryEmbed: () => <div data-testid="telemetry-embed" />,
}));

describe('mfe-patient-consultation App', () => {
  beforeEach(() => {
    sessionStorage.setItem('telemedicine.session', JSON.stringify(sessionState));
  });

  it('renders consultation layout when journey gate allows access', () => {
    renderWithProviders(<App />, { route: '/patient/consultation' });
    expect(screen.getByRole('heading', { level: 2, name: 'Consultation' })).toBeInTheDocument();
    expect(screen.getByLabelText('Video')).toBeInTheDocument();
    expect(screen.getByLabelText('Chat')).toBeInTheDocument();
  });
});
