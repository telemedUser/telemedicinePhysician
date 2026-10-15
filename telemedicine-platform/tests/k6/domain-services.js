import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 25,
  duration: '2m',
  thresholds: {
    http_req_duration: ['p(95)<300'],
    checks: ['rate>0.95'],
  },
};

const BASE = __ENV.GATEWAY_URL || 'http://localhost:8080';
const APPOINTMENT_ID = __ENV.APPOINTMENT_ID || 'apt-load-test';

const domainEndpoints = [
  { name: 'waiting-room queue', url: `/api/v1/waiting-room/queue/${APPOINTMENT_ID}` },
  { name: 'identity status', url: `/api/v1/identity/status/${APPOINTMENT_ID}` },
  { name: 'video sessions', url: `/api/v1/video/sessions/${APPOINTMENT_ID}` },
  { name: 'telemetry status', url: `/api/v1/telemetry/status/${APPOINTMENT_ID}` },
  { name: 'clinical docs', url: `/api/v1/clinical-docs/${APPOINTMENT_ID}` },
  { name: 'survey', url: `/api/v1/survey/${APPOINTMENT_ID}` },
  { name: 'reporting kpis', url: '/api/v1/reporting/kpis' },
  { name: 'monitoring snapshot', url: '/api/v1/monitoring/snapshot' },
  { name: 'config read', url: '/api/v1/config/video.provider' },
  { name: 'audit health', url: '/api/v1/audit/health' },
];

export default function domainServices() {
  const endpoint = domainEndpoints[__ITER % domainEndpoints.length];
  const res = http.get(`${BASE}${endpoint.url}`);
  check(res, {
    [`${endpoint.name} 200`]: (r) => r.status === 200,
    [`${endpoint.name} p95`]: (r) => r.timings.duration < 300,
  });
  sleep(0.3);
}
