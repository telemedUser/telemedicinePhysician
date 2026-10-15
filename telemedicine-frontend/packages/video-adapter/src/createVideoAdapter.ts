import { createDefaultApiClient } from '@telemedicine/api-client';
import { ConfigKeys } from '@telemedicine/shared-types';
import type { VideoProvider } from '@telemedicine/shared-types';
import { useProvider } from '@telemedicine/feature-flags';
import { AcsVideoAdapter } from './AcsVideoAdapter';
import { SimulatorVideoAdapter } from './SimulatorVideoAdapter';
import type { VideoAdapter } from './types';
import { VonageVideoAdapter } from './VonageVideoAdapter';

export function createVideoAdapter(provider: VideoProvider = 'simulator'): VideoAdapter {
  switch (provider) {
    case 'vonage':
      return new VonageVideoAdapter(createDefaultApiClient());
    case 'acs':
      return new AcsVideoAdapter(createDefaultApiClient());
    case 'simulator':
      return new SimulatorVideoAdapter();
    default:
      console.warn(`[video-adapter] Unknown provider "${provider}", falling back to simulator`);
      return new SimulatorVideoAdapter();
  }
}

/** Resolves the active provider from feature-flags and returns a VideoAdapter instance. */
export function useVideoAdapter(): VideoAdapter {
  const provider = useProvider(ConfigKeys.VIDEO_PROVIDER, 'simulator') as VideoProvider;
  return createVideoAdapter(provider);
}
