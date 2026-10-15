import { describe, expect, it } from 'vitest';
import { SimulatorTelemetryAdapter } from './SimulatorTelemetryAdapter';
import { TelemetryResults } from './TelemetryResults';
import { render, screen } from '@testing-library/react';

describe('telemetry-adapter', () => {
  it('returns realistic vital sign ranges from simulator', async () => {
    const adapter = new SimulatorTelemetryAdapter();
    await adapter.initialize({});
    await adapter.startMeasurement('appt-1');
    const results = await adapter.stopMeasurement();
    expect(results.heartRate).toBeGreaterThanOrEqual(60);
    expect(results.heartRate).toBeLessThanOrEqual(100);
    expect(results.respiratoryRate).toBeGreaterThanOrEqual(12);
    expect(results.respiratoryRate).toBeLessThanOrEqual(20);
    adapter.destroy();
  });

  it('renders telemetry results', () => {
    render(
      <TelemetryResults
        results={{
          sessionId: 's1',
          measuredAt: new Date().toISOString(),
          heartRate: 72,
          spo2: 98,
        }}
      />,
    );
    expect(screen.getByTestId('telemetry-results')).toBeInTheDocument();
    expect(screen.getByText('72 bpm')).toBeInTheDocument();
  });
});
