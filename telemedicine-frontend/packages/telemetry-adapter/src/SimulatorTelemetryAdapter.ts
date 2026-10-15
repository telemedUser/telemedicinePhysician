import type { TelemetryAdapter, TelemetryConfig, TelemetryResults, VitalSigns } from './types';

function randomInRange(min: number, max: number): number {
  return Math.round(min + Math.random() * (max - min));
}

/** Generates fake measurements with realistic vital sign ranges. */
export class SimulatorTelemetryAdapter implements TelemetryAdapter {
  private config: TelemetryConfig = {};
  private results: TelemetryResults | null = null;
  private vitalsTimer: number | null = null;
  private completeListeners = new Set<(results: TelemetryResults) => void>();
  private errorListeners = new Set<(error: Error) => void>();

  async initialize(config: TelemetryConfig): Promise<void> {
    this.config = config;
  }

  async startMeasurement(appointmentId: string): Promise<void> {
    const sessionId = crypto.randomUUID();
    this.results = null;
    this.vitalsTimer = window.setInterval(() => {
      const vitals: VitalSigns = {
        heartRate: randomInRange(60, 100),
        respiratoryRate: randomInRange(12, 20),
        spo2: randomInRange(95, 99),
        stressLevel: randomInRange(1, 5),
        bloodPressureSystolic: randomInRange(110, 130),
        bloodPressureDiastolic: randomInRange(70, 85),
      };
      this.results = { ...vitals, sessionId, measuredAt: new Date().toISOString() };
    }, 1000);
    void appointmentId;
  }

  async stopMeasurement(): Promise<TelemetryResults> {
    if (this.vitalsTimer) {
      window.clearInterval(this.vitalsTimer);
      this.vitalsTimer = null;
    }
    if (!this.results) {
      this.results = {
        sessionId: crypto.randomUUID(),
        measuredAt: new Date().toISOString(),
        heartRate: randomInRange(60, 100),
        respiratoryRate: randomInRange(12, 20),
        spo2: randomInRange(95, 99),
        stressLevel: randomInRange(1, 5),
      };
    }
    this.completeListeners.forEach((cb) => cb(this.results!));
    return this.results;
  }

  getResults(): TelemetryResults | null {
    return this.results;
  }

  onMeasurementComplete(callback: (results: TelemetryResults) => void): () => void {
    this.completeListeners.add(callback);
    return () => this.completeListeners.delete(callback);
  }

  onError(callback: (error: Error) => void): () => void {
    this.errorListeners.add(callback);
    return () => this.errorListeners.delete(callback);
  }

  isSupported(): boolean {
    return true;
  }

  destroy(): void {
    if (this.vitalsTimer) window.clearInterval(this.vitalsTimer);
    this.completeListeners.clear();
    this.errorListeners.clear();
    this.results = null;
  }

  /** Test helper — push an error to listeners. */
  emitError(error: Error): void {
    this.errorListeners.forEach((cb) => cb(error));
  }
}
