import { createDefaultApiClient } from '@telemedicine/api-client';
import { ConfigKeys } from '@telemedicine/shared-types';
import type { ChatProvider } from '@telemedicine/shared-types';
import { useProvider } from '@telemedicine/feature-flags';
import { ApiChatAdapter } from './ApiChatAdapter';
import { SimulatorChatAdapter } from './SimulatorChatAdapter';
import type { ClinicalChatAdapter } from './types';

export function createChatAdapter(provider: ChatProvider = 'simulator'): ClinicalChatAdapter {
  switch (provider) {
    case 'vonage':
    case 'acs':
      return new ApiChatAdapter(createDefaultApiClient());
    case 'simulator':
      return new SimulatorChatAdapter();
    default:
      return new SimulatorChatAdapter();
  }
}

export function useChatAdapter(): ClinicalChatAdapter {
  const provider = useProvider(ConfigKeys.CHAT_PROVIDER, 'simulator') as ChatProvider;
  return createChatAdapter(provider);
}
