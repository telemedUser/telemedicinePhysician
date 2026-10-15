import { SimulatorVideoAdapter } from '@telemedicine/video-adapter';
import { SimulatorChatAdapter } from '@telemedicine/chat-adapter';
import { SimulatorTelemetryAdapter } from '@telemedicine/telemetry-adapter';

export const mockAdapters = {
  video: () => new SimulatorVideoAdapter(),
  chat: () => new SimulatorChatAdapter(),
  telemetry: () => new SimulatorTelemetryAdapter(),
};

export { SimulatorVideoAdapter, SimulatorChatAdapter, SimulatorTelemetryAdapter };
