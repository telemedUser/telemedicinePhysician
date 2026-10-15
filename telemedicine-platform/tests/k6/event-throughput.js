import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 40,
  duration: '90s',
  thresholds: {
    http_req_duration: ['p(95)<300'],
    checks: ['rate>0.90'],
  },
};

const BASE = __ENV.GATEWAY_URL || 'http://localhost:8080';

export default function eventThroughput() {
  const appointmentId = `apt-event-${__VU}-${__ITER}`;

  const createBody = JSON.stringify({ patientId: 'pat-event', physicianId: 'phy-event' });
  const created = http.post(`${BASE}/v1/appointments/v2`, createBody, {
    headers: { 'Content-Type': 'application/json' },
  });
  check(created, { 'appointment created': (r) => r.status >= 200 && r.status < 300 });

  const linkBody = JSON.stringify({ appointmentId, channel: 'sms' });
  const link = http.post(`${BASE}/api/v1/waiting-room/session-link`, linkBody, {
    headers: { 'Content-Type': 'application/json' },
  });
  check(link, { 'link generated event path': (r) => r.status >= 200 && r.status < 300 });

  const sessionBody = JSON.stringify({ appointmentId });
  const video = http.post(`${BASE}/api/v1/video/sessions`, sessionBody, {
    headers: { 'Content-Type': 'application/json' },
  });
  check(video, { 'video session event path': (r) => r.status >= 200 && r.status < 300 });

  const snapshot = http.get(`${BASE}/api/v1/monitoring/snapshot`);
  check(snapshot, {
    'monitoring reflects events': (r) => r.status === 200,
    'event path p95': (r) => r.timings.duration < 300,
  });

  sleep(0.1);
}
