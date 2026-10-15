export type BandwidthClass = 'excellent' | 'good' | 'fair' | 'poor';

export interface DeviceTestResult {
  passed: boolean;
  label: string;
  detail?: string;
  warning?: boolean;
}

export interface PreflightReport {
  camera: DeviceTestResult;
  microphone: DeviceTestResult;
  speaker: DeviceTestResult;
  bandwidth: DeviceTestResult;
  allPassed: boolean;
  bandwidthMbps?: number;
  bandwidthClass?: BandwidthClass;
}

export function classifyBandwidth(mbps: number): BandwidthClass {
  if (mbps >= 5) return 'excellent';
  if (mbps >= 2) return 'good';
  if (mbps >= 1) return 'fair';
  return 'poor';
}

export async function testCamera(deviceId?: string): Promise<DeviceTestResult> {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({
      video: deviceId ? { deviceId: { exact: deviceId } } : true,
    });
    stream.getTracks().forEach((t) => t.stop());
    return { passed: true, label: 'Camera' };
  } catch (err) {
    const error = err as DOMException;
    return {
      passed: false,
      label: 'Camera',
      detail: error.name === 'NotAllowedError' ? 'Permission denied' : error.message,
    };
  }
}

export async function testMicrophone(deviceId?: string): Promise<DeviceTestResult> {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({
      audio: deviceId ? { deviceId: { exact: deviceId } } : true,
    });
    stream.getTracks().forEach((t) => t.stop());
    return { passed: true, label: 'Microphone' };
  } catch (err) {
    const error = err as DOMException;
    return {
      passed: false,
      label: 'Microphone',
      detail: error.name === 'NotFoundError' ? 'No microphone found' : error.message,
    };
  }
}

export async function testSpeaker(): Promise<DeviceTestResult> {
  try {
    const ctx = new AudioContext();
    const oscillator = ctx.createOscillator();
    const gain = ctx.createGain();
    gain.gain.value = 0.05;
    oscillator.connect(gain);
    gain.connect(ctx.destination);
    oscillator.start();
    await new Promise((r) => setTimeout(r, 300));
    oscillator.stop();
    await ctx.close();
    return { passed: true, label: 'Speaker' };
  } catch (err) {
    return { passed: false, label: 'Speaker', detail: (err as Error).message };
  }
}

export async function testBandwidth(
  testUrl = '/api/v1/health/payload',
  payloadBytes = 100_000,
): Promise<DeviceTestResult & { mbps?: number; bandwidthClass?: BandwidthClass }> {
  try {
    const start = performance.now();
    const res = await fetch(`${testUrl}?bytes=${payloadBytes}`, { cache: 'no-store' });
    if (!res.ok) {
      return { passed: false, label: 'Bandwidth', detail: `HTTP ${res.status}` };
    }
    await res.arrayBuffer();
    const seconds = (performance.now() - start) / 1000;
    const mbps = (payloadBytes * 8) / seconds / 1_000_000;
    const bandwidthClass = classifyBandwidth(mbps);
    const warning = bandwidthClass === 'fair' || bandwidthClass === 'poor';
    return {
      passed: bandwidthClass !== 'poor',
      label: 'Bandwidth',
      detail: `${mbps.toFixed(1)} Mbps (${bandwidthClass})`,
      warning,
      mbps,
      bandwidthClass,
    };
  } catch (err) {
    return { passed: false, label: 'Bandwidth', detail: (err as Error).message };
  }
}

export async function runPreflightChecks(options?: {
  cameraDeviceId?: string;
  microphoneDeviceId?: string;
  bandwidthTestUrl?: string;
}): Promise<PreflightReport> {
  const [camera, microphone, speaker, bandwidth] = await Promise.all([
    testCamera(options?.cameraDeviceId),
    testMicrophone(options?.microphoneDeviceId),
    testSpeaker(),
    testBandwidth(options?.bandwidthTestUrl),
  ]);
  return {
    camera,
    microphone,
    speaker,
    bandwidth,
    allPassed: camera.passed && microphone.passed && speaker.passed && bandwidth.passed,
    bandwidthMbps: bandwidth.mbps,
    bandwidthClass: bandwidth.bandwidthClass,
  };
}

export async function submitDeviceTestResults(
  appointmentId: string,
  report: PreflightReport,
  fetchFn: typeof fetch = fetch,
): Promise<void> {
  const res = await fetchFn(`/api/v1/waiting-room/device-test/${appointmentId}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Idempotency-Key': crypto.randomUUID() },
    body: JSON.stringify(report),
  });
  if (!res.ok) {
    throw new Error(`Device test submission failed: HTTP ${res.status}`);
  }
}
