import type { ConsentRecord, JourneyStatus } from '@telemedicine/shared-types';

export function createAppointment(overrides: Record<string, unknown> = {}) {
  return {
    appointmentId: 'appt-1',
    patientId: 'patient-1',
    physicianId: 'physician-1',
    status: 'LINK_GENERATED' as JourneyStatus,
    scheduledAt: new Date().toISOString(),
    ...overrides,
  };
}

export function createConsent(overrides: Partial<ConsentRecord> = {}): ConsentRecord {
  return {
    type: 'platform',
    version: '1.0.0',
    status: 'granted',
    grantedAt: new Date().toISOString(),
    ...overrides,
  };
}

export function createVideoSession(overrides: Record<string, unknown> = {}) {
  return {
    sessionId: 'video-session-1',
    appointmentId: 'appt-1',
    provider: 'simulator',
    token: 'mock-token',
    ...overrides,
  };
}

export function createSurvey(overrides: Record<string, unknown> = {}) {
  return {
    surveyId: 'survey-1',
    appointmentId: 'appt-1',
    status: 'pending',
    ...overrides,
  };
}

export const fixtures = {
  createAppointment,
  createConsent,
  createVideoSession,
  createSurvey,
};
