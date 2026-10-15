import type { TelemetryResults } from './types';

export interface TelemetryResultsProps {
  results: TelemetryResults | null;
}

export function TelemetryResults({ results }: TelemetryResultsProps) {
  if (!results) {
    return <p data-testid="telemetry-results-empty">No measurement results yet.</p>;
  }

  const metrics = [
    { label: 'Heart rate', value: results.heartRate, unit: 'bpm' },
    { label: 'SpO₂', value: results.spo2, unit: '%' },
    { label: 'Respiratory rate', value: results.respiratoryRate, unit: '/min' },
    { label: 'Stress level', value: results.stressLevel, unit: '/5' },
    {
      label: 'Blood pressure',
      value:
        results.bloodPressureSystolic && results.bloodPressureDiastolic
          ? `${results.bloodPressureSystolic}/${results.bloodPressureDiastolic}`
          : undefined,
      unit: 'mmHg',
    },
  ];

  return (
    <section data-testid="telemetry-results" aria-label="Telemetry results">
      <h3>Vital Signs</h3>
      <dl style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem' }}>
        {metrics
          .filter((m) => m.value !== undefined)
          .map((m) => (
            <div key={m.label}>
              <dt style={{ fontSize: '0.75rem', color: '#6b7280' }}>{m.label}</dt>
              <dd style={{ margin: 0, fontWeight: 600 }}>
                {m.value} {m.unit}
              </dd>
            </div>
          ))}
      </dl>
    </section>
  );
}
