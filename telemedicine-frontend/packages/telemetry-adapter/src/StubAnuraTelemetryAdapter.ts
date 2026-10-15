import type { TelemetryAdapter, TelemetryConfig, TelemetryResults } from './types';

const DEFAULT_SCRIPT_URL = 'https://cdn.anura.ai/sdk/anura.min.js';

/** Loads ANURA SDK script dynamically and wraps measurement lifecycle. */
export class StubAnuraTelemetryAdapter implements TelemetryAdapter {
  private config: TelemetryConfig = {};
  private results: TelemetryResults | null = null;
  private scriptLoaded = false;
  private completeListeners = new Set<(results: TelemetryResults) => void>();
  private errorListeners = new Set<(error: Error) => void>();

  async initialize(config: TelemetryConfig): Promise<void> {
    this.config = config;
    await this.loadScript(config.scriptUrl ?? DEFAULT_SCRIPT_URL);
  }

  async startMeasurement(appointmentId: string): Promise<void> {
    if (!this.scriptLoaded) {
      throw new Error('ANURA SDK not loaded');
    }
    this.results = {
      sessionId: crypto.randomUUID(),
      measuredAt: new Date().toISOString(),
      heartRate: 72,
      spo2: 98,
    };
    void appointmentId;
    void this.config;
  }

  async stopMeasurement(): Promise<TelemetryResults> {
    if (!this.results) {
      const err = new Error('No active measurement');
      this.errorListeners.forEach((cb) => cb(err));
      throw err;
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
    return typeof document !== 'undefined';
  }

  destroy(): void {
    this.completeListeners.clear();
    this.errorListeners.clear();
    this.results = null;
  }

  private loadScript(url: string): Promise<void> {
    return new Promise((resolve, reject) => {
      if (typeof document === 'undefined') {
        reject(new Error('Document not available'));
        return;
      }
      const existing = document.querySelector(`script[data-anura-sdk]`);
      if (existing) {
        this.scriptLoaded = true;
        resolve();
        return;
      }
      const script = document.createElement('script');
      script.src = url;
      script.async = true;
      script.dataset.anuraSdk = 'true';
      script.onload = () => {
        this.scriptLoaded = true;
        resolve();
      };
      script.onerror = () => {
        const err = new Error(`Failed to load ANURA SDK from ${url}`);
        this.errorListeners.forEach((cb) => cb(err));
        reject(err);
      };
      document.head.appendChild(script);
    });
  }
}
