import { useCallback, useEffect } from 'react';
import { reportMetric } from './reporting';

export function useTimingMark(stepName: string, enabled = true): () => void {
  const markStart = useCallback(() => {
    if (!enabled || typeof performance === 'undefined') return;
    performance.mark(`${stepName}:start`);
  }, [stepName, enabled]);

  const markEnd = useCallback(() => {
    if (!enabled || typeof performance === 'undefined') return;
    performance.mark(`${stepName}:end`);
    try {
      performance.measure(stepName, `${stepName}:start`, `${stepName}:end`);
      const entries = performance.getEntriesByName(stepName, 'measure');
      const latest = entries[entries.length - 1];
      if (latest) {
        void reportMetric({ name: `journey.${stepName}`, value: latest.duration, unit: 'ms' });
      }
    } catch {
      /* measure may fail if start mark missing */
    }
  }, [stepName, enabled]);

  useEffect(() => markStart, [markStart]);

  return markEnd;
}
