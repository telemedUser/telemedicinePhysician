export interface VitalSigns {
  heartRate?: number;
  bloodPressureSystolic?: number;
  bloodPressureDiastolic?: number;
  respiratoryRate?: number;
  spo2?: number;
  stressLevel?: number;
}

export interface TelemetryResults extends VitalSigns {
  sessionId: string;
  measuredAt: string;
}

export interface TelemetryConfig {
  apiKey?: string;
  participantId?: string;
  scriptUrl?: string;
}

export interface TelemetryAdapter {
  initialize(config: TelemetryConfig): Promise<void>;
  startMeasurement(appointmentId: string): Promise<void>;
  stopMeasurement(): Promise<TelemetryResults>;
  getResults(): TelemetryResults | null;
  onMeasurementComplete(callback: (results: TelemetryResults) => void): () => void;
  onError(callback: (error: Error) => void): () => void;
  isSupported(): boolean;
  destroy(): void;
}
