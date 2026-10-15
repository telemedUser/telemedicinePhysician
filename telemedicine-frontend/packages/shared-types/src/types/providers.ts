export type VideoProvider = 'vonage' | 'acs' | 'simulator';
export type ChatProvider = 'vonage' | 'acs' | 'simulator';

export interface ProviderConfig {
  video: VideoProvider;
  chat: ChatProvider;
  telemetryEnabled: boolean;
}
