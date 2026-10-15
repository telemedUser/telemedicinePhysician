import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.SIMULATOR_URL || 'http://localhost:8200';

export const options = {
  vus: 50,
  duration: '30s',
  thresholds: {
    http_req_duration: ['p(95)<300'],
  },
};

export function setup() {
  const vendors = [
    { vendor: 'his', latencyMs: 150 },
    { vendor: 'payment', latencyMs: 80 },
    { vendor: 'vonage', latencyMs: 50 },
  ];
  for (const item of vendors) {
    http.put(`${BASE}/sim/admin/vendors/${item.vendor}`, JSON.stringify(item), {
      headers: { 'Content-Type': 'application/json' },
    });
  }
  http.post(`${BASE}/sim/admin/scenarios/happy`);
}

export default function () {
  const appointmentId = `apt-k6-${__VU}-${__ITER}`;
  const his = http.get(`${BASE}/sim/his/appointments/${appointmentId}`);
  check(his, { 'his 200': (r) => r.status === 200 });

  const payment = http.get(`${BASE}/sim/payment/validate/${appointmentId}`);
  check(payment, { 'payment 200': (r) => r.status === 200 });

  const session = http.post(
    `${BASE}/sim/vonage/session`,
    JSON.stringify({ appointmentId }),
    { headers: { 'Content-Type': 'application/json' } },
  );
  check(session, { 'vonage 200': (r) => r.status === 200 });
  sleep(0.1);
}
