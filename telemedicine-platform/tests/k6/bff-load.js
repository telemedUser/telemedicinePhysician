import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 30,
  duration: '2m',
  thresholds: {
    http_req_duration: ['p(95)<300'],
    checks: ['rate>0.95'],
  },
};

const BASE = __ENV.GATEWAY_URL || 'http://localhost:8080';
const APPOINTMENT_ID = __ENV.APPOINTMENT_ID || 'apt-load-test';

export default function bffLoad() {
  const patientJourney = http.get(`${BASE}/bff/patient/journey/${APPOINTMENT_ID}`);
  check(patientJourney, {
    'patient bff 200': (r) => r.status === 200,
    'patient bff p95': (r) => r.timings.duration < 300,
  });

  const physicianWorklist = http.get(`${BASE}/bff/physician/worklist`);
  check(physicianWorklist, {
    'physician bff 200': (r) => r.status === 200,
    'physician bff p95': (r) => r.timings.duration < 300,
  });

  const adminSnapshot = http.get(`${BASE}/bff/admin/snapshot`);
  check(adminSnapshot, {
    'admin bff 200': (r) => r.status === 200,
    'admin bff p95': (r) => r.timings.duration < 300,
  });

  sleep(0.5);
}
