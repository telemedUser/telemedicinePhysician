export type JourneyStatus =
  | 'LINK_GENERATED'
  | 'CONSENT_GRANTED'
  | 'IDENTITY_VALIDATED'
  | 'DEVICE_TEST_COMPLETED'
  | 'OPEN_PATIENT'
  | 'CHECKED_IN'
  | 'OPEN_DOCTOR'
  | 'IN_CONSULTATION'
  | 'CONSULTATION_CLOSED'
  | 'SURVEY_COMPLETED';

export type JourneyStep =
  | 'consent'
  | 'identity'
  | 'waiting-room'
  | 'consultation'
  | 'documents'
  | 'survey'
  | 'complete';

export const JOURNEY_STATUS_ORDER: JourneyStatus[] = [
  'LINK_GENERATED',
  'CONSENT_GRANTED',
  'IDENTITY_VALIDATED',
  'DEVICE_TEST_COMPLETED',
  'OPEN_PATIENT',
  'CHECKED_IN',
  'OPEN_DOCTOR',
  'IN_CONSULTATION',
  'CONSULTATION_CLOSED',
  'SURVEY_COMPLETED',
];

export function hasReachedStatus(current: JourneyStatus, required: JourneyStatus): boolean {
  const currentIdx = JOURNEY_STATUS_ORDER.indexOf(current);
  const requiredIdx = JOURNEY_STATUS_ORDER.indexOf(required);
  return currentIdx >= 0 && requiredIdx >= 0 && currentIdx >= requiredIdx;
}

export interface AppointmentContext {
  appointmentId: string;
  patientId?: string;
  physicianId?: string;
  status: JourneyStatus;
}
