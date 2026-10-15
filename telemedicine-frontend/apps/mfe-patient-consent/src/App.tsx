import { useState } from 'react';
import { Navigate } from 'react-router-dom';
import {
  PlatformConsent,
  ClinicalConsent,
  ConsentWithdrawal,
  useConsent,
  consentText,
} from '@telemedicine/consent-kit';
import { useSession, useJourneyGate } from '@telemedicine/session-context';
import { Button, StatusBadge, PageLayout, ErrorMessage, EmptyState, type BadgeVariant } from '@telemedicine/design-system';
import { useTranslation } from '@telemedicine/i18n';

type ConsentStep = 'platform' | 'clinical' | 'status';

function statusVariant(status: string): BadgeVariant {
  if (status === 'granted') return 'ok';
  if (status === 'withdrawn') return 'degraded';
  return 'pending';
}

function JourneyBlocked() {
  const { t } = useTranslation('consent');
  return <EmptyState title={t('blocked.title')} description={t('blocked.description')} />;
}

export default function App() {
  const allowed = useJourneyGate('LINK_GENERATED');
  const { t } = useTranslation('consent');
  const { setJourneyStatus } = useSession();
  const { grantPlatformConsent, grantClinicalConsent, platformStatus, clinicalStatus, withdrawConsent, loading, error } =
    useConsent();
  const [step, setStep] = useState<ConsentStep>('platform');
  const [accepted, setAccepted] = useState<Record<string, boolean>>({});
  const [clinicalAccepted, setClinicalAccepted] = useState(false);

  if (!allowed) {
    return <JourneyBlocked />;
  }

  if (clinicalStatus === 'granted' && step !== 'status') {
    return <Navigate to="/patient/identity" replace />;
  }

  const allPlatformRequired =
    consentText.platform.items.filter((i) => i.required).every((i) => accepted[i.id]) &&
    Object.keys(accepted).length > 0;

  if (step === 'platform') {
    return (
      <PageLayout>
        <h2>{t('platform.heading')}</h2>
        <StatusBadge variant={statusVariant(platformStatus)}>{platformStatus}</StatusBadge>
        <PlatformConsent accepted={accepted} onChange={(id, v) => setAccepted((p) => ({ ...p, [id]: v }))} />
        {error && <ErrorMessage message={error.message} />}
        <Button
          disabled={!allPlatformRequired || loading}
          onClick={() =>
            void grantPlatformConsent().then(() => {
              setJourneyStatus('CONSENT_GRANTED');
              setStep('clinical');
            })
          }
        >
          {t('platform.continue')}
        </Button>
        <Button variant="ghost" onClick={() => setStep('status')}>
          {t('status.heading')}
        </Button>
      </PageLayout>
    );
  }

  if (step === 'clinical') {
    return (
      <PageLayout>
        <h2>{t('clinical.heading')}</h2>
        <StatusBadge variant={statusVariant(clinicalStatus)}>{clinicalStatus}</StatusBadge>
        <ClinicalConsent accepted={clinicalAccepted} onChange={setClinicalAccepted} />
        {error && <ErrorMessage message={error.message} />}
        <Button disabled={!clinicalAccepted || loading} onClick={() => void grantClinicalConsent()}>
          {t('clinical.continue')}
        </Button>
      </PageLayout>
    );
  }

  return (
    <PageLayout>
      <h2>{t('status.heading')}</h2>
      <p>
        {t('status.platform')}: <StatusBadge variant={statusVariant(platformStatus)}>{platformStatus}</StatusBadge>
      </p>
      <p>
        {t('status.clinical')}: <StatusBadge variant={statusVariant(clinicalStatus)}>{clinicalStatus}</StatusBadge>
      </p>
      <ConsentWithdrawal onWithdraw={withdrawConsent} loading={loading} />
      <Button variant="secondary" onClick={() => setStep('platform')}>
        {t('platform.heading')}
      </Button>
    </PageLayout>
  );
}
