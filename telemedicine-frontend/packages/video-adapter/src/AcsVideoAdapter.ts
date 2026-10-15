import type { ApiClient } from '@telemedicine/api-client';
import type { VideoRole, VideoSessionInfo } from './types';
import { BaseVideoAdapter } from './types';

/** Mock Azure Communication Services wrapper — maps session/token from video-service API. */
export class AcsVideoAdapter extends BaseVideoAdapter {
  private readonly client: ApiClient;
  private callAgent: { join: (token: string) => Promise<void>; leave: () => void } | null = null;

  constructor(client: ApiClient) {
    super();
    this.client = client;
  }

  async createSession(appointmentId: string): Promise<VideoSessionInfo> {
    const res = await this.client.post<{ sessionId: string; token: string }>(
      `/v1/video/sessions`,
      { appointmentId, provider: 'acs' },
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
    this.callAgent = createMockAcsCallAgent();
    await this.callAgent.join(this.session.token ?? '');
    this.emitParticipantJoined({ id: 'local', displayName: 'You', role });
    this.setNetworkQuality(4);
  }

  async leaveSession(): Promise<void> {
    this.callAgent?.leave();
    this.callAgent = null;
    await super.leaveSession();
  }

  destroy(): void {
    this.callAgent?.leave();
    this.callAgent = null;
    super.destroy();
  }
}

function createMockAcsCallAgent() {
  return {
    join: async (_token: string) => {
      /* mock ACS join */
    },
    leave: () => {
      /* mock ACS leave */
    },
  };
}
