import type { BadgeVariant } from '@telemedicine/design-system';
import type {
  ConsultationStatus,
  ProviderStatus,
  Readiness,
  TechnicalStatus,
} from '../api/types';

export function readinessVariant(readiness: Readiness): BadgeVariant {
  switch (readiness) {
    case 'READY':
      return 'ok';
    case 'IN_PROGRESS':
      return 'pending';
    default:
      return 'neutral';
  }
}

export function technicalVariant(status: TechnicalStatus): BadgeVariant {
  switch (status) {
    case 'OK':
      return 'ok';
    case 'DEGRADED':
      return 'degraded';
    case 'IMPAIRED':
      return 'degraded';
    default:
      return 'neutral';
  }
}

export function consultationVariant(status: ConsultationStatus): BadgeVariant {
  switch (status) {
    case 'STARTED':
    case 'OPEN_DOCTOR':
      return 'ok';
    case 'CHECKED_IN':
      return 'pending';
    case 'CLOSED':
      return 'neutral';
    default:
      return 'info';
  }
}

export function providerStatusVariant(status: ProviderStatus): BadgeVariant {
  switch (status) {
    case 'ACTIVE':
      return 'ok';
    case 'ON_LEAVE':
      return 'pending';
    case 'SUSPENDED':
    case 'OFFBOARDED':
      return 'degraded';
    default:
      return 'neutral';
  }
}

export function formatWaiting(seconds: number): string {
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  if (mins === 0) return `${secs}s`;
  return `${mins}m ${String(secs).padStart(2, '0')}s`;
}
