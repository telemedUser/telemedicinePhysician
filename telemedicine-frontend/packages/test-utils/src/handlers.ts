import { http, HttpResponse } from 'msw';
import { fixtures } from './fixtures';

export const handlers = {
  patientJourney: (appointmentId = 'appt-1') =>
    http.get(`/api/bff/patient/journey/${appointmentId}`, () =>
      HttpResponse.json({
        data: {
          appointmentId,
          journeyStatus: 'CONSENT_GRANTED',
          identity: { status: 'OK' },
          waitingRoom: { status: 'PENDING' },
          video: { status: 'PENDING' },
          documents: { status: 'PENDING' },
          survey: { status: 'PENDING' },
        },
      }),
    ),

  consent: () => [
    http.post('/api/v1/waiting-room/consent/platform', () =>
      HttpResponse.json({ data: fixtures.createConsent({ type: 'platform' }) }),
    ),
    http.post('/api/v1/waiting-room/consent/clinical', () =>
      HttpResponse.json({ data: fixtures.createConsent({ type: 'clinical' }) }),
    ),
    http.post('/api/v1/waiting-room/consent/withdraw', () =>
      HttpResponse.json({ data: fixtures.createConsent({ status: 'withdrawn' }) }),
    ),
  ],

  identity: () =>
    http.post('/api/v1/identity/verify', () =>
      HttpResponse.json({ data: { verified: true } }),
    ),

  video: (sessionId = 'session-1') => [
    http.post('/api/v1/video/sessions', () =>
      HttpResponse.json({ data: fixtures.createVideoSession() }),
    ),
    http.get(`/api/v1/video/sessions/${sessionId}/chat`, () =>
      HttpResponse.json({ data: [] }),
    ),
    http.post(`/api/v1/video/sessions/${sessionId}/chat`, async ({ request }) => {
      const body = (await request.json()) as { body: string; clinical?: boolean };
      return HttpResponse.json({
        data: {
          id: crypto.randomUUID(),
          senderId: 'local',
          senderRole: 'patient',
          body: body.body,
          sentAt: new Date().toISOString(),
          clinical: body.clinical,
        },
      });
    }),
  ],

  deviceTest: (appointmentId = 'appt-1') =>
    http.post(`/api/v1/waiting-room/device-test/${appointmentId}`, () =>
      HttpResponse.json({ data: { accepted: true } }),
    ),

  config: () =>
    http.get('/api/v1/config', () =>
      HttpResponse.json({
        data: {
          'video.provider': 'simulator',
          'chat.provider': 'simulator',
          'telemetry.enabled': false,
        },
      }),
    ),

  monitoring: () => [
    http.post('/api/v1/monitoring/frontend-errors', () => HttpResponse.json({ data: { ok: true } })),
    http.post('/api/v1/monitoring/metrics', () => HttpResponse.json({ data: { ok: true } })),
  ],

  all: () => [
    handlers.patientJourney(),
    ...handlers.consent(),
    handlers.identity(),
    ...handlers.video(),
    handlers.deviceTest(),
    handlers.config(),
    ...handlers.monitoring(),
  ],
};
