const MAX_REPORTS_PER_MINUTE = 10;
const reportTimestamps: number[] = [];

export function isDevMode(): boolean {
  if (typeof import.meta !== 'undefined') {
    const mode = import.meta.env?.VITE_OBSERVABILITY_MODE;
    if (mode === 'dev') return true;
    if (mode === 'prod') return false;
    return import.meta.env?.DEV === true;
  }
  return false;
}

export function isRateLimited(): boolean {
  const now = Date.now();
  while (reportTimestamps.length && now - reportTimestamps[0]! > 60_000) {
    reportTimestamps.shift();
  }
  return reportTimestamps.length >= MAX_REPORTS_PER_MINUTE;
}

export function recordReport(): void {
  reportTimestamps.push(Date.now());
}

export interface ErrorReport {
  message: string;
  stack?: string;
  componentStack?: string;
  correlationId?: string;
  journeyStep?: string;
  userAgent?: string;
}

export interface MetricReport {
  name: string;
  value: number;
  unit?: string;
  tags?: Record<string, string>;
}

async function postToMonitoring(path: string, body: unknown): Promise<void> {
  if (isDevMode()) {
    console.info('[observability-web:dev]', path, body);
    return;
  }
  if (isRateLimited()) {
    console.warn('[observability-web] rate limit reached, dropping report');
    return;
  }
  recordReport();
  await fetch(`/api/v1/monitoring${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
}

export async function reportError(report: ErrorReport): Promise<void> {
  if (isDevMode()) {
    console.error('[observability-web:dev]', report);
    return;
  }
  await postToMonitoring('/frontend-errors', {
    ...report,
    userAgent: report.userAgent ?? navigator.userAgent,
    timestamp: new Date().toISOString(),
  });
}

export async function reportMetric(metric: MetricReport): Promise<void> {
  if (isDevMode()) {
    console.info('[observability-web:dev:metric]', metric);
    return;
  }
  await postToMonitoring('/metrics', {
    ...metric,
    timestamp: new Date().toISOString(),
  });
}

/** Reset rate limiter — test helper only. */
export function resetRateLimiter(): void {
  reportTimestamps.length = 0;
}
