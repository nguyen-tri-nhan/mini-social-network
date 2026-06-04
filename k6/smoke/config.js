export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const THRESHOLDS = {
  http_req_failed:   ['rate<0.01'],     // < 1% requests fail
  http_req_duration: ['p(95)<2000'],    // 95% responses < 2s
};
