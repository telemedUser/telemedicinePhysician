import type { ApiClient } from '@telemedicine/api-client';
import type { VideoRole, VideoSessionInfo } from './types';
import { BaseVideoAdapter } from './types';

/** Mock Vonage/OpenTok SDK wrapper — maps session/token from video-service API. */
export class VonageVideoAdapter extends BaseVideoAdapter {
  private readonly client: ApiClient;
  private sdkSession: { connect: (token: string) => Promise<void>; disconnect: () => void } | null =
    null;

  constructor(client: ApiClient) {
    super();
    this.client = client;
  }

  async createSession(appointmentId: string): Promise<VideoSessionInfo> {
    const res = await this.client.post<{ sessionId: string; token: string }>(
      `/v1/video/sessions`,
      { appointmentId, provider: 'vonage' },
    );
    this.session = {
      sessionId: res.data.sessionId,
      appointmentId,
      token: res.data.token,
    };
    return this.session;
  }

  async joinSession(sessionId: string, role: VideoRole): Promise<void> {
    if (!this.session || this.session.sessionId !== sessionId) {
      throw new Error(`Session not found: ${sessionId}`);
    }
    this.role = role;
    this.sdkSession = createMockVonageSession();
    await this.sdkSession.connect(this.session.token ?? '');
    this.emitParticipantJoined({ id: 'local', displayName: 'You', role });
    this.setNetworkQuality(4);
  }

  async leaveSession(): Promise<void> {
    this.sdkSession?.disconnect();
    this.sdkSession = null;
    await super.leaveSession();
  }

  destroy(): void {
    this.sdkSession?.disconnect();
    this.sdkSession = null;
    super.destroy();
  }
}

function createMockVonageSession() {
  return {
    connect: async (_token: string) => {
      /* mock SDK connect */
    },
    disconnect: () => {
      /* mock SDK disconnect */
    },
  };
}
