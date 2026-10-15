import { useTranslation } from '@telemedicine/i18n';
import { Button } from '@telemedicine/design-system';

export interface ConsentWithdrawalProps {
  onWithdraw: () => Promise<void>;
  loading?: boolean;
}

export function ConsentWithdrawal({ onWithdraw, loading = false }: ConsentWithdrawalProps) {
  const { t } = useTranslation('consent');

  return (
    <section aria-label={t('withdrawal.sectionLabel')} data-testid="consent-withdrawal">
      <h3>{t('withdrawal.heading')}</h3>
      <p>{t('withdrawal.description')}</p>
      <Button variant="danger" onClick={() => void onWithdraw()} disabled={loading}>
        {loading ? t('withdrawal.submitting') : t('withdrawal.submit')}
      </Button>
    </section>
  );
}
