import type { ReactNode } from 'react';
import { useTranslation } from '@telemedicine/i18n';
import consentText from './consent-text';

export interface ConsentItem {
  id: string;
  title: string;
  body: ReactNode;
  required: boolean;
}

export interface PlatformConsentProps {
  items?: ConsentItem[];
  accepted: Record<string, boolean>;
  onChange: (id: string, accepted: boolean) => void;
  version?: string;
}

export function PlatformConsent({ items, accepted, onChange, version }: PlatformConsentProps) {
  const { t } = useTranslation('consent');
  const resolvedItems =
    items ??
    consentText.platform.items.map((item) => ({
      id: item.id,
      title: t(item.titleKey.replace('consent.', '')),
      body: t(item.bodyKey.replace('consent.', '')),
      required: item.required,
    }));
  const displayVersion = version ?? consentText.platform.version;

  return (
    <section aria-label={t('platform.sectionLabel')}>
      <h2>{t('platform.heading')}</h2>
      <p style={{ fontSize: '0.875rem', color: '#6b7280' }}>
        {t('platform.versionLabel')}: {displayVersion}
      </p>
      <ul style={{ listStyle: 'none', padding: 0 }}>
        {resolvedItems.map((item) => (
          <li key={item.id} style={{ marginBottom: '1rem' }}>
            <label>
              <input
                type="checkbox"
                checked={accepted[item.id] ?? false}
                onChange={(e) => onChange(item.id, e.target.checked)}
                required={item.required}
              />{' '}
              <strong>{item.title}</strong>
            </label>
            <div style={{ marginTop: '0.25rem', color: '#4b5563' }}>{item.body}</div>
          </li>
        ))}
      </ul>
    </section>
  );
}
