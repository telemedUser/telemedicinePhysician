import { describe, expect, it } from 'vitest';
import { classifyBandwidth } from './preflight';

describe('device-test', () => {
  it('classifies bandwidth tiers', () => {
    expect(classifyBandwidth(8)).toBe('excellent');
    expect(classifyBandwidth(3)).toBe('good');
    expect(classifyBandwidth(1.5)).toBe('fair');
    expect(classifyBandwidth(0.5)).toBe('poor');
  });
});
