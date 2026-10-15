import { useTranslation } from '@telemedicine/i18n';
import consentText from './consent-text';

export interface ClinicalConsentProps {
  procedure?: string;
  risks?: string[];
  accepted: boolean;
  onChange: (accepted: boolean) => void;
  version?: string;
}

export function ClinicalConsent({
  procedure,
  risks,
  accepted,
  onChange,
  version,
}: ClinicalConsentProps) {
  const { t } = useTranslation('consent');
  const resolvedProcedure = procedure ?? t('clinical.procedure');
  const resolvedRisks =
    risks ??
    consentText.clinical.risksKeys.map((key) => t(key.replace('consent.', '')));
  const displayVersion = version ?? consentText.clinical.version;

  return (
    <section aria-label={t('clinical.sectionLabel')}>
      <h2>
        {t('clinical.heading')}: {resolvedProcedure}
      </h2>
      <p style={{ fontSize: '0.875rem', color: '#6b7280' }}>
        {t('clinical.versionLabel')}: {displayVersion}
      </p>
      <ul>
        {resolvedRisks.map((risk) => (
          <li key={risk}>{risk}</li>
        ))}
      </ul>
      <label>
        <input type="checkbox" checked={accepted} onChange={(e) => onChange(e.target.checked)} />{' '}
        {t('clinical.acceptLabel')}
      </label>
    </section>
  );
}
