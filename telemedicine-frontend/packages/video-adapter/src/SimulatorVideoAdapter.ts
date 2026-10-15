import type { VideoParticipant, VideoRole, VideoSessionInfo } from './types';
import { BaseVideoAdapter } from './types';

/** Local mock with fake participant events and simulated network quality. */
export class SimulatorVideoAdapter extends BaseVideoAdapter {
  private qualityTimer: number | null = null;

  async createSession(appointmentId: string): Promise<VideoSessionInfo> {
    this.session = {
      sessionId: crypto.randomUUID(),
      appointmentId,
      token: `sim-token-${appointmentId}`,
    };
    return this.session;
  }

  async joinSession(sessionId: string, role: VideoRole): Promise<void> {
    if (!this.session || this.session.sessionId !== sessionId) {
      throw new Error(`Session not found: ${sessionId}`);
    }
    this.role = role;
    this.emitParticipantJoined({ id: 'local', displayName: 'You (simulated)', role });
    this.startQualitySimulation();
  }

  /** Test helper — simulate a remote participant joining. */
  simulateJoin(participant: VideoParticipant): void {
    if (this.session) {
      this.emitParticipantJoined(participant);
    }
  }

  /** Test helper — simulate a remote participant leaving. */
  simulateLeave(participant: VideoParticipant): void {
    if (this.session) {
      this.emitParticipantLeft(participant);
    }
  }

  /** Test helper — simulate transient disconnect and reconnect. */
  simulateReconnect(): void {
    this.emitReconnecting();
    window.setTimeout(() => this.setNetworkQuality(4), 500);
  }

  destroy(): void {
    this.stopQualitySimulation();
    super.destroy();
  }

  private startQualitySimulation(): void {
    this.stopQualitySimulation();
    this.qualityTimer = window.setInterval(() => {
      const next = (Math.floor(Math.random() * 3) + 3) as 3 | 4 | 5;
      this.setNetworkQuality(next);
    }, 5000);
  }

  private stopQualitySimulation(): void {
    if (this.qualityTimer) {
      window.clearInterval(this.qualityTimer);
      this.qualityTimer = null;
    }
  }
}
