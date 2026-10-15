import { describe, expect, it } from 'vitest';
import { hasReachedStatus } from '@telemedicine/shared-types';

describe('session-context journey gates', () => {
  it('validates status progression', () => {
    expect(hasReachedStatus('OPEN_DOCTOR', 'CONSENT_GRANTED')).toBe(true);
    expect(hasReachedStatus('CONSENT_GRANTED', 'OPEN_DOCTOR')).toBe(false);
  });
});
