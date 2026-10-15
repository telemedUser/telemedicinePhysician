import type { VideoProvider } from '@telemedicine/shared-types';

export type { VideoProvider };

export type VideoRole = 'patient' | 'physician' | 'observer';

export type NetworkQuality = 1 | 2 | 3 | 4 | 5;

export interface VideoParticipant {
  id: string;
  displayName: string;
  role?: VideoRole;
}

export interface VideoSessionInfo {
  sessionId: string;
  appointmentId: string;
  token?: string;
}

export interface VideoAdapter {
  createSession(appointmentId: string): Promise<VideoSessionInfo>;
  joinSession(sessionId: string, role: VideoRole): Promise<void>;
  leaveSession(): Promise<void>;
  muteAudio(muted: boolean): Promise<void>;
  muteVideo(muted: boolean): Promise<void>;
  switchCamera(deviceId: string): Promise<void>;
  switchMicrophone(deviceId: string): Promise<void>;
  getNetworkQuality(): NetworkQuality;
  onParticipantJoined(callback: (participant: VideoParticipant) => void): () => void;
  onParticipantLeft(callback: (participant: VideoParticipant) => void): () => void;
  onNetworkQualityChanged(callback: (quality: NetworkQuality) => void): () => void;
  onReconnecting(callback: () => void): () => void;
  onDisconnected(callback: () => void): () => void;
  destroy(): void;
}

export type Unsubscribe = () => void;

export abstract class BaseVideoAdapter implements VideoAdapter {
  protected session: VideoSessionInfo | null = null;
  protected role: VideoRole | null = null;
  protected audioMuted = false;
  protected videoMuted = false;
  protected cameraDeviceId: string | null = null;
  protected microphoneDeviceId: string | null = null;
  protected networkQuality: NetworkQuality = 5;
  protected destroyed = false;

  private participantJoinedListeners = new Set<(p: VideoParticipant) => void>();
  private participantLeftListeners = new Set<(p: VideoParticipant) => void>();
  private networkQualityListeners = new Set<(q: NetworkQuality) => void>();
  private reconnectingListeners = new Set<() => void>();
  private disconnectedListeners = new Set<() => void>();

  abstract createSession(appointmentId: string): Promise<VideoSessionInfo>;
  abstract joinSession(sessionId: string, role: VideoRole): Promise<void>;

  async leaveSession(): Promise<void> {
    this.session = null;
    this.role = null;
  }

  async muteAudio(muted: boolean): Promise<void> {
    this.audioMuted = muted;
  }

  async muteVideo(muted: boolean): Promise<void> {
    this.videoMuted = muted;
  }

  async switchCamera(deviceId: string): Promise<void> {
    this.cameraDeviceId = deviceId;
  }

  async switchMicrophone(deviceId: string): Promise<void> {
    this.microphoneDeviceId = deviceId;
  }

  getNetworkQuality(): NetworkQuality {
    return this.networkQuality;
  }

  onParticipantJoined(callback: (participant: VideoParticipant) => void): Unsubscribe {
    this.participantJoinedListeners.add(callback);
    return () => this.participantJoinedListeners.delete(callback);
  }

  onParticipantLeft(callback: (participant: VideoParticipant) => void): Unsubscribe {
    this.participantLeftListeners.add(callback);
    return () => this.participantLeftListeners.delete(callback);
  }

  onNetworkQualityChanged(callback: (quality: NetworkQuality) => void): Unsubscribe {
    this.networkQualityListeners.add(callback);
    return () => this.networkQualityListeners.delete(callback);
  }

  onReconnecting(callback: () => void): Unsubscribe {
    this.reconnectingListeners.add(callback);
    return () => this.reconnectingListeners.delete(callback);
  }

  onDisconnected(callback: () => void): Unsubscribe {
    this.disconnectedListeners.add(callback);
    return () => this.disconnectedListeners.delete(callback);
  }

  destroy(): void {
    this.destroyed = true;
    void this.leaveSession();
    this.participantJoinedListeners.clear();
    this.participantLeftListeners.clear();
    this.networkQualityListeners.clear();
    this.reconnectingListeners.clear();
    this.disconnectedListeners.clear();
  }

  protected emitParticipantJoined(participant: VideoParticipant): void {
    this.participantJoinedListeners.forEach((cb) => cb(participant));
  }

  protected emitParticipantLeft(participant: VideoParticipant): void {
    this.participantLeftListeners.forEach((cb) => cb(participant));
  }

  protected setNetworkQuality(quality: NetworkQuality): void {
    this.networkQuality = quality;
    this.networkQualityListeners.forEach((cb) => cb(quality));
  }

  protected emitReconnecting(): void {
    this.reconnectingListeners.forEach((cb) => cb());
  }

  protected emitDisconnected(): void {
    this.disconnectedListeners.forEach((cb) => cb());
  }
}
