import { ConfigKeys } from '@telemedicine/shared-types';
import { useFeatureFlag } from '@telemedicine/feature-flags';
import { SimulatorTelemetryAdapter } from './SimulatorTelemetryAdapter';
import { StubAnuraTelemetryAdapter } from './StubAnuraTelemetryAdapter';
import type { TelemetryAdapter } from './types';

export function createTelemetryAdapter(enabled = false): TelemetryAdapter {
  if (enabled) {
    return new StubAnuraTelemetryAdapter();
  }
  return new SimulatorTelemetryAdapter();
}

export function useTelemetryAdapter(): TelemetryAdapter {
  const { enabled } = useFeatureFlag(ConfigKeys.TELEMETRY_ENABLED, false);
  return createTelemetryAdapter(enabled);
}
