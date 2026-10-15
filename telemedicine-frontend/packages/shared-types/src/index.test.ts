import { describe, expect, it } from 'vitest';
import {
  ErrorCode,
  synthesizeProblemDetail,
  isProblemDetail,
  hasReachedStatus,
  JOURNEY_STATUS_ORDER,
} from './index';

describe('shared-types', () => {
  it('synthesizes ProblemDetail', () => {
    const problem = synthesizeProblemDetail(503, ErrorCode.SERVICE_UNAVAILABLE, 'down');
    expect(problem.status).toBe(503);
    expect(problem.code).toBe(ErrorCode.SERVICE_UNAVAILABLE);
    expect(isProblemDetail(problem)).toBe(true);
  });

  it('tracks journey status order', () => {
    expect(hasReachedStatus('OPEN_DOCTOR', 'CONSENT_GRANTED')).toBe(true);
    expect(hasReachedStatus('CONSENT_GRANTED', 'OPEN_DOCTOR')).toBe(false);
    expect(JOURNEY_STATUS_ORDER).toHaveLength(10);
  });
});
