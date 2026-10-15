import { Button } from '@telemedicine/design-system';
import type { DeviceTestResult } from './preflight';
import { DeviceSelector } from './DeviceSelector';
import { useDeviceTest } from './useDeviceTest';

const REMEDIATION: Record<string, string> = {
  Camera: 'Grant camera permission in browser settings or close other apps using the camera.',
  Microphone: 'Check your microphone connection and grant permission when prompted.',
  Speaker: 'Ensure your speakers or headphones are connected and volume is turned up.',
  Bandwidth: 'Close other video calls or move closer to your Wi-Fi router to improve bandwidth.',
};

function ResultRow({ result, onRetest }: { result: DeviceTestResult; onRetest?: () => void }) {
  const icon = result.passed ? '✓' : result.warning ? '!' : '✗';
  const color = result.passed ? '#059669' : result.warning ? '#d97706' : '#dc2626';

  return (
    <li style={{ marginBottom: '0.75rem' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
        <span style={{ color, fontWeight: 700 }} aria-hidden="true">
          {icon}
        </span>
        <strong>{result.label}</strong>
        {result.detail && <span style={{ color: '#6b7280' }}>— {result.detail}</span>}
        {onRetest && (
          <button type="button" onClick={onRetest} style={{ marginLeft: 'auto' }}>
            Re-test
          </button>
        )}
      </div>
      {!result.passed && (
        <p style={{ margin: '0.25rem 0 0 1.5rem', fontSize: '0.875rem', color: '#4b5563' }}>
          {REMEDIATION[result.label] ?? 'Check your device settings and try again.'}
        </p>
      )}
    </li>
  );
}

export interface DeviceTestPanelProps {
  appointmentId?: string;
}

export function DeviceTestPanel({ appointmentId }: DeviceTestPanelProps) {
  const {
    report,
    devices,
    cameraDeviceId,
    microphoneDeviceId,
    loading,
    refreshDevices,
    runTests,
    submitResults,
    selectCamera,
    selectMicrophone,
  } = useDeviceTest(appointmentId);

  return (
    <section aria-label="Device preflight test" data-testid="device-test-panel">
      <h2>Device Check</h2>
      <DeviceSelector
        devices={devices}
        selectedCameraId={cameraDeviceId}
        selectedMicrophoneId={microphoneDeviceId}
        onCameraChange={selectCamera}
        onMicrophoneChange={selectMicrophone}
      />
      <div style={{ display: 'flex', gap: '0.5rem', margin: '1rem 0' }}>
        <Button onClick={() => void refreshDevices()} variant="secondary">
          Refresh devices
        </Button>
        <Button onClick={() => void runTests()} loading={loading}>
          Run tests
        </Button>
        {report?.allPassed && (
          <Button onClick={() => void submitResults()} variant="primary">
            Submit results
          </Button>
        )}
      </div>
      {report && (
        <ul style={{ listStyle: 'none', padding: 0 }}>
          <ResultRow result={report.camera} onRetest={() => void runTests()} />
          <ResultRow result={report.microphone} onRetest={() => void runTests()} />
          <ResultRow result={report.speaker} onRetest={() => void runTests()} />
          <ResultRow result={report.bandwidth} onRetest={() => void runTests()} />
        </ul>
      )}
    </section>
  );
}
