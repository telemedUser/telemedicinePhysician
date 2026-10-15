import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import type { JourneyStatus } from '@telemedicine/shared-types';
import { useJourneyGate } from '@telemedicine/session-context';
import { EmptyState } from '@telemedicine/design-system';
import { useTranslation } from '@telemedicine/i18n';

export function JourneyGateRoute({
  requiredStatus,
  redirectTo,
  children,
}: {
  requiredStatus: JourneyStatus;
  redirectTo: string;
  children: ReactNode;
}) {
  const allowed = useJourneyGate(requiredStatus);
  const { t } = useTranslation('common');

  if (!allowed) {
    return (
      <>
        <EmptyState
        title={t('stepBlocked')}
        description={t('stepBlockedHint')}
        />
        <Navigate to={redirectTo} replace />
      </>
    );
  }

  return children;
}
