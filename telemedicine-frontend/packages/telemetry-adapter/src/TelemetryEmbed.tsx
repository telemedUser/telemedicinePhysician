import { useEffect, useRef } from 'react';
import { TelemetryConfigGate, TelemetryConsentGate } from './TelemetryGates';
import { useTelemetryAdapter } from './createTelemetryAdapter';

export interface TelemetryEmbedProps {
  appointmentId: string;
  consentGranted?: boolean;
  onComplete?: () => void;
}

export function TelemetryEmbed({ appointmentId, consentGranted, onComplete }: TelemetryEmbedProps) {
  const adapter = useTelemetryAdapter();
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    let active = true;
    void (async () => {
      try {
        await adapter.initialize({ participantId: appointmentId });
        await adapter.startMeasurement(appointmentId);
      } catch {
        /* errors surfaced via onError */
      }
    })();
    const unsub = adapter.onMeasurementComplete(() => {
      if (active) onComplete?.();
    });
    return () => {
      active = false;
      unsub();
      adapter.destroy();
    };
  }, [adapter, appointmentId, onComplete]);

  return (
    <TelemetryConfigGate>
      <TelemetryConsentGate consentGranted={consentGranted}>
        <div
          ref={containerRef}
          data-testid="telemetry-embed"
          aria-label="Telemetry measurement"
          style={{
            minHeight: 240,
            border: '1px dashed #d1d5db',
            borderRadius: 8,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            background: '#fafafa',
          }}
        >
          Camera-based vital sign measurement
        </div>
      </TelemetryConsentGate>
    </TelemetryConfigGate>
  );
}
