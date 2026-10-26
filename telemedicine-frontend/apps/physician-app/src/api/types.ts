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
  /** The slot time the patient booked (from the patient service), shown as the consultation Time. */
  scheduledAt?: string;
  waitingSeconds: number;
  readiness: Readiness;
  consultationStatus: ConsultationStatus;
  technicalStatus: TechnicalStatus;
  telemetryAvailable: boolean;
  flags: WorklistFlag[];
  redFlagAcknowledged: boolean;
}

/** One quick-check / triage question as carried in the clinical-context document. */
export interface QuickCheckItem {
  question: string;
  answer: string;
  concerning?: boolean;
}

/** One medical-history line. */
export interface MedicalHistoryItem {
  label: string;
  detail?: string;
}

/** One previous encounter. */
export interface PreviousEncounterItem {
  date?: string;
  diagnosis?: string;
  physician?: string;
  modality?: string;
}

/** One attachment reference. */
export interface AttachmentItem {
  filename?: string;
  sizeLabel?: string;
  capturedAt?: string;
}

/**
 * The variable-length clinical context carried in the event's clinicalContextJson and parsed by the
 * physician service. All fields are optional — the physician view renders only what arrived.
 */
export interface ClinicalContext {
  reasonText?: string;
  symptoms?: string[];
  quickCheck?: QuickCheckItem[];
  medicalHistory?: MedicalHistoryItem[];
  previousEncounters?: PreviousEncounterItem[];
  attachments?: AttachmentItem[];
  sourced?: string;
}

/**
 * GET /api/v1/physicians/me/worklist/{appointmentId} — the D-03 review & join detail. Every field
 * is projected from the patient service's ConsultationContextPublished event plus the live worklist
 * row; nothing here is hard-coded on the physician side.
 */
export interface PatientDetail {
  appointmentId: string;
  patientDisplay?: string;
  ageYears: number;
  sexCode?: string | null;
  city?: string | null;
  insuranceScheme?: string | null;
  nationalIdMasked?: string | null;
  modality?: string | null;
  visitType?: string | null;
  scheduledAt?: string | null;
  severityScore: number;
  symptomDurationCode?: string | null;
  symptomCount: number;
  attachmentCount: number;
  redFlag: boolean;
  consultationFeeMinor: number;
  voucherMinor: number;
  amountPaidMinor: number;
  currency: string;
  paymentStatus?: string | null;
  clinicalContext?: ClinicalContext | null;
  readiness: Readiness;
  consultationStatus: ConsultationStatus;
  technicalStatus: TechnicalStatus;
  enteredAt?: string | null;
  redFlagAcknowledged: boolean;
  contextAvailable: boolean;
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

/* ---------- D-08 care plan + reference directories ---------- */

/** Options returned by the DB-backed reference directory search endpoints. */
export interface DiagnosisOption {
  code: string;
  title: string;
  system: string;
}
export interface MedicationOption {
  id: string;
  name: string;
  strength?: string | null;
  form?: string | null;
}
export interface DiagnosticTestOption {
  id: string;
  name: string;
  category?: string | null;
  specimen?: string | null;
}

/** The four-section clinical note. */
export interface ClinicalNote {
  history?: string;
  findings?: string;
  assessment?: string;
  plan?: string;
}

/** A selected diagnosis on the care plan. */
export interface CarePlanDiagnosis {
  code: string;
  title: string;
  primary: boolean;
}

/** A prescribed medication line. */
export interface CarePlanPrescription {
  medicationId?: string;
  name: string;
  strength?: string;
  form?: string;
  dosage?: string;
  frequency?: string;
  durationDays?: number | null;
  quantity?: number | null;
  instructions?: string;
}

/** An ordered diagnostic test. */
export interface CarePlanDiagnosticTest {
  testId?: string;
  name: string;
  category?: string;
  specimen?: string;
  note?: string;
}

export interface CarePlanFollowUp {
  enabled: boolean;
  mode?: string;
  inDays?: number | null;
  date?: string;
  instructions?: string;
}

export interface CarePlanMedicalLeave {
  enabled: boolean;
  reason?: string;
  startDate?: string;
  endDate?: string;
}

export interface CarePlanSummary {
  text?: string;
  instructions?: string;
}

/** GET/PUT /api/v1/physicians/me/consultations/{id}/care-plan */
export interface CarePlan {
  appointmentId: string;
  note: ClinicalNote;
  diagnoses: CarePlanDiagnosis[];
  prescriptions: CarePlanPrescription[];
  diagnosticTests: CarePlanDiagnosticTest[];
  followUp: CarePlanFollowUp;
  medicalLeave: CarePlanMedicalLeave;
  summary: CarePlanSummary;
  status: 'DRAFT' | 'APPROVED';
}
