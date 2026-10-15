export type DomainEventType =
  | 'AppointmentCreated'
  | 'AppointmentRescheduled'
  | 'PaymentConfirmed'
  | 'LinkGenerated'
  | 'ConsentGranted'
  | 'ConsentWithdrawn'
  | 'IdentityValidated'
  | 'IdentityValidationFailed'
  | 'DeviceTestCompleted'
  | 'PatientEnteredWaitingRoom'
  | 'StatusChanged'
  | 'VideoSessionCreated'
  | 'VideoSessionStarted'
  | 'VideoSessionEnded'
  | 'ChatMessageSent'
  | 'TelemetryMeasured'
  | 'ConnectionQualityDegraded'
  | 'ClinicalDocumentIssued'
  | 'PrescriptionIssued'
  | 'MedicalLeaveIssued'
  | 'ConsultationClosed'
  | 'SurveyRequested'
  | 'SurveyCompleted'
  | 'NotificationSent'
  | 'NotificationFailed'
  | 'IntegrationFailed'
  | 'ConfigurationChanged'
  | 'AuditRecorded';

export interface EventEnvelope<T = unknown> {
  eventId: string;
  type: DomainEventType;
  version: string;
  occurredAt: string;
  correlationId: string;
  causationId?: string;
  subject: string;
  payload: T;
}

export interface StatusChangedPayload {
  appointmentId: string;
  status: string;
  previousStatus?: string;
}

export interface ConsentEventPayload {
  appointmentId: string;
  consentType: string;
  version: string;
}

export interface VideoSessionEventPayload {
  sessionId: string;
  appointmentId: string;
  provider?: string;
}

export interface SurveyCompletedPayload {
  appointmentId: string;
  score: number;
}
