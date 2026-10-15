import http from 'k6/http';
import { check, group, sleep } from 'k6';

export const options = {
  vus: 20,
  duration: '2m',
  thresholds: {
    http_req_duration: ['p(95)<300'],
    checks: ['rate>0.95'],
  },
};

const BASE = __ENV.GATEWAY_URL || 'http://localhost:8080';

export function setup() {
  const createBody = JSON.stringify({ patientId: 'pat-k6', physicianId: 'phy-k6' });
  const created = http.post(`${BASE}/v1/appointments/v2`, createBody, {
    headers: { 'Content-Type': 'application/json' },
  });
  const body = created.json();
  const appointmentId = body?.data?.id || body?.id || 'apt-load-test';
  return { appointmentId };
}

export default function patientJourney(data) {
  const appointmentId = data.appointmentId;

  group('pre-consultation', () => {
    const journey = http.get(`${BASE}/bff/patient/journey/${appointmentId}`);
    check(journey, {
      'journey status 200': (r) => r.status === 200,
      'journey p95 under 300ms': (r) => r.timings.duration < 300,
    });

    const payment = http.post(`${BASE}/v1/appointments/v2/${appointmentId}/payment/validate`);
    check(payment, { 'payment validate 200': (r) => r.status === 200 || r.status === 204 });

    const linkBody = JSON.stringify({ appointmentId, channel: 'email' });
    const link = http.post(`${BASE}/api/v1/waiting-room/session-link`, linkBody, {
      headers: { 'Content-Type': 'application/json' },
    });
    check(link, { 'session link 2xx': (r) => r.status >= 200 && r.status < 300 });

    const queue = http.get(`${BASE}/api/v1/waiting-room/queue/${appointmentId}`);
    check(queue, { 'queue 200': (r) => r.status === 200 });
  });

  group('consultation', () => {
    const sessionBody = JSON.stringify({ appointmentId });
    const video = http.post(`${BASE}/api/v1/video/sessions`, sessionBody, {
      headers: { 'Content-Type': 'application/json' },
    });
    check(video, { 'video session 2xx': (r) => r.status >= 200 && r.status < 300 });

    const telemetryBody = JSON.stringify({ appointmentId });
    const telemetry = http.post(`${BASE}/api/v1/telemetry/token`, telemetryBody, {
      headers: { 'Content-Type': 'application/json' },
    });
    check(telemetry, { 'telemetry token 2xx': (r) => r.status >= 200 && r.status < 300 });
  });

  group('post-consultation', () => {
    const docs = http.get(`${BASE}/api/v1/clinical-docs/${appointmentId}`);
    check(docs, { 'clinical docs 200': (r) => r.status === 200 });

    const survey = http.get(`${BASE}/api/v1/survey/${appointmentId}`);
    check(survey, { 'survey availability 200': (r) => r.status === 200 });

    const kpis = http.get(`${BASE}/api/v1/reporting/kpis`);
    check(kpis, { 'kpis status 200': (r) => r.status === 200 });
  });

  sleep(1);
}
