/**
 * Types mirroring the physician-service OpenAPI contract
 * (shared/contracts/physician-service.yaml) and the physician-bff aggregation.
 * These are app-local until promoted into @telemedicine/shared-types.
 */

export type Readiness = 'NOT_READY' | 'IN_PROGRESS' | 'READY';
export type TechnicalStatus = 'OK' | 'DEGRADED' | 'IMPAIRED';
export type ConsultationStatus = 'CHECKED_IN' | 'OPEN_DOCTOR' | 'STARTED' | 'CLOSED' | 'UNKNOWN';
export type WorklistFlag = 'FIRST_VISIT' | 'FOLLOW_UP' | 'GUEST' | 'RED_FLAG';
export type ProviderStatus = 'ACTIVE' | 'SUSPENDED' | 'ON_LEAVE' | 'OFFBOARDED';
export type ConsultationMode = 'video' | 'audio' | 'chat';

/** GET /api/v1/physicians/me/worklist */
export interface WorklistEntry {
  appointmentId: string;
  patientRef?: string;
  mode?: ConsultationMode;
  enteredAt?: string;
  waitingSeconds: number;
  readiness: Readiness;
  consultationStatus: ConsultationStatus;
  technicalStatus: TechnicalStatus;
  telemetryAvailable: boolean;
  flags: WorklistFlag[];
  redFlagAcknowledged: boolean;
}

/** POST /api/v1/physicians/me/consultations/{id}/close */
export type CompletionDocument =
  | 'NOTE'
  | 'DIAGNOSIS'
  | 'PRESCRIPTION'
  | 'CERTIFICATE'
  | 'REFERRAL'
  | 'FOLLOW_UP';

export interface CloseConsultationRequest {
  completedDocuments: CompletionDocument[];
  exceptionReason?: string;
}

export interface CloseConsultationResponse {
  appointmentId: string;
  closed: boolean;
  minimumSatisfied: boolean;
  missingDocuments: string[];
}

/** GET /api/v1/physicians and /api/v1/physicians/{id} */
export interface ProviderSummary {
  providerId: string;
  displayName: string;
  status: ProviderStatus;
  specialties: string[];
  languages: string[];
  modes: ConsultationMode[];
}

export interface ProviderCredential {
  type: string;
  number?: string;
  validUntil?: string;
  verified: boolean;
}

export interface AvailabilityWindow {
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  mode: ConsultationMode;
  maxConcurrent: number;
}

export interface ProviderMetrics {
  utilization?: number;
  avgDurationSeconds?: number;
  avgDocumentationSeconds?: number;
  noShowRate?: number;
  technicalFailureRate?: number;
}

export interface ProviderDetail extends ProviderSummary {
  credentials: ProviderCredential[];
  availability: AvailabilityWindow[];
  metrics?: ProviderMetrics;
  sourceRef?: string;
  lastRefreshedAt?: string;
  stale?: boolean;
}

export interface ProviderStatusRequest {
  status: ProviderStatus;
  reason?: string;
}
