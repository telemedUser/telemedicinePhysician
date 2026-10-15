import { useEffect, type ReactNode } from 'react';
import { onCLS, onINP, onLCP, onTTFB, type Metric } from 'web-vitals';
import { reportError, reportMetric } from './reporting';

export interface ObservabilityProviderProps {
  children: ReactNode;
  enableWebVitals?: boolean;
}

function handleWebVital(metric: Metric): void {
  void reportMetric({
    name: `web-vital.${metric.name}`,
    value: metric.value,
    unit: metric.name === 'CLS' ? 'score' : 'ms',
  });
}

export function ObservabilityProvider({ children, enableWebVitals = true }: ObservabilityProviderProps) {
  useEffect(() => {
    const onError = (event: ErrorEvent) => {
      void reportError({ message: event.message, stack: event.error?.stack });
    };
    const onRejection = (event: PromiseRejectionEvent) => {
      const reason = event.reason;
      void reportError({
        message: reason instanceof Error ? reason.message : String(reason),
        stack: reason instanceof Error ? reason.stack : undefined,
      });
    };
    window.addEventListener('error', onError);
    window.addEventListener('unhandledrejection', onRejection);
    return () => {
      window.removeEventListener('error', onError);
      window.removeEventListener('unhandledrejection', onRejection);
    };
  }, []);

  useEffect(() => {
    if (!enableWebVitals) return;
    onLCP(handleWebVital);
    onINP(handleWebVital);
    onCLS(handleWebVital);
    onTTFB(handleWebVital);
  }, [enableWebVitals]);

  return <>{children}</>;
}
