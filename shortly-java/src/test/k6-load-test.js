// K6 Load Test for Shortly Backend
// Usage: k6 run k6-load-test.js
// Install: https://k6.io/docs/get-started/installation/

import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 50 },  // Ramp up to 50 users
    { duration: '1m', target: 100 },  // Stay at 100 users
    { duration: '30s', target: 0 },   // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<200'], // 95% of requests < 200ms
    http_req_failed: ['rate<0.01'],   // Error rate < 1%
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export function setup() {
  // Create a shortlink for testing redirects
  const payload = JSON.stringify({
    url: 'https://example.org/test',
    expiresAt: new Date(Date.now() + 3600000).toISOString(),
  });

  const res = http.post(`${BASE_URL}/api/shorten`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });

  return { shortCode: res.body };
}

export default function (data) {
  // Test redirect performance (most common operation)
  const redirectRes = http.get(`${BASE_URL}/${data.shortCode}`, {
    redirects: 0, // Don't follow redirects
  });

  check(redirectRes, {
    'redirect is 302': (r) => r.status === 302,
    'redirect has location': (r) => r.headers['Location'] !== undefined,
  });

  sleep(0.1); // 100ms pause between requests
}
