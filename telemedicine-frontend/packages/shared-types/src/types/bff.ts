import type { JourneyStatus } from './journey';

export type SectionStatus = 'OK' | 'DEGRADED' | 'PENDING';

export interface JourneySection<T = Record<string, unknown>> {
  status: SectionStatus;
  data?: T;
}

export interface PatientJourneyResponse {
  appointmentId: string;
  overallStatus: 'OK' | 'DEGRADED';
  journeyStatus?: JourneyStatus;
  consent: JourneySection;
  identity: JourneySection;
  waitingRoom: JourneySection;
  video: JourneySection;
  documents: JourneySection;
  survey: JourneySection;
}

export type AggregationStatus = 'OK' | 'DEGRADED';

export interface PhysicianWorklistItem {
  appointmentId: string;
  patientName?: string;
  status: string;
  scheduledAt?: string;
  patientId?: string;
  sourceStatus?: AggregationStatus;
}

export interface PhysicianWorklistResponse {
  status: AggregationStatus;
  entries: PhysicianWorklistItem[];
}

export interface VideoJoinPayload {
  status: AggregationStatus;
  sessionId?: string | null;
  token?: string | null;
}

export interface ChatContext {
  status: AggregationStatus;
  threadId?: string | null;
}

export interface TelemetryContext {
  status: AggregationStatus;
  enabled?: boolean;
}

export interface PatientContextSummary {
  status: AggregationStatus;
  patientId?: string | null;
  name?: string | null;
  source?: string | null;
  dateOfBirth?: string | null;
  medications?: string[];
  allergies?: string[];
  clinicalHistory?: string;
}

export interface PhysicianConsultationResponse {
  appointmentId: string;
  status: AggregationStatus;
  video: VideoJoinPayload;
  chat: ChatContext;
  telemetry: TelemetryContext;
  patientContext: PatientContextSummary;
}

export interface MedicationEntry {
  drugName: string;
  dosage: string;
  frequency: string;
}

export interface PhysicianDocumentationPayload {
  diagnosis?: {
    icd10?: string;
    notes?: string;
  };
  prescription?: {
    medications?: MedicationEntry[];
  };
  medicalLeave?: {
    startDate?: string;
    endDate?: string;
    notes?: string;
  };
}

export interface AdminSnapshotResponse {
  configVersion?: string;
  lastAuditSequence?: number;
  schedulerHealthy?: boolean;
  kpis?: Record<string, unknown>;
  activeSessions?: number;
  waitingRoomQueue?: number;
  errorRate?: number;
}
