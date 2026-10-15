import type { ReactNode } from 'react';
import { ConfigKeys } from '@telemedicine/shared-types';
import { useFeatureFlag } from '@telemedicine/feature-flags';
import { useSession } from '@telemedicine/session-context';

export interface TelemetryConsentGateProps {
  children: ReactNode;
  consentGranted?: boolean;
  fallback?: ReactNode;
}

/** Blocks telemetry UI until clinical consent is granted. */
export function TelemetryConsentGate({
  children,
  consentGranted,
  fallback = <p role="status">Clinical consent is required before telemetry measurement.</p>,
}: TelemetryConsentGateProps) {
  const { state } = useSession();
  const granted =
    consentGranted ??
    (state.metadata.clinicalConsentGranted === true || state.journeyStatus === 'CONSENT_GRANTED');

  if (!granted) return <>{fallback}</>;
  return <>{children}</>;
}

export interface TelemetryConfigGateProps {
  children: ReactNode;
  fallback?: ReactNode;
}

/** Blocks telemetry UI when feature flag disables telemetry. */
export function TelemetryConfigGate({
  children,
  fallback = null,
}: TelemetryConfigGateProps) {
  const { enabled, loading } = useFeatureFlag(ConfigKeys.TELEMETRY_ENABLED, false);
  if (loading) return null;
  if (!enabled) return <>{fallback}</>;
  return <>{children}</>;
}
