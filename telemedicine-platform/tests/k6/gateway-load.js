import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 50 },
    { duration: '1m', target: 100 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<300'],
    checks: ['rate>0.98'],
  },
};

const BASE = __ENV.GATEWAY_URL || 'http://localhost:8080';

export default function gatewayLoad() {
  const health = http.get(`${BASE}/actuator/health`);
  check(health, { 'health 200': (r) => r.status === 200 });

  const openapi = http.get(`${BASE}/v3/api-docs`);
  check(openapi, { 'openapi 200': (r) => r.status === 200 });

  const snapshot = http.get(`${BASE}/api/v1/monitoring/snapshot`);
  check(snapshot, { 'monitoring snapshot 200': (r) => r.status === 200 });

  sleep(0.2);
}
