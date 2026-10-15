import { useCallback, useEffect, useState } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import { useApiClient } from '@telemedicine/api-client';
import {
  Button,
  EmptyState,
  ErrorMessage,
  Inline,
  KpiCard,
  LoadingSpinner,
  PageLayout,
  Stack,
} from '@telemedicine/design-system';
import { useTranslation } from '@telemedicine/i18n';
import type { PhysicianConsultationResponse } from '@telemedicine/shared-types';

export default function App() {
  const { t } = useTranslation('physicianPatientContext');
  const { appointmentId: routeId } = useParams<{ appointmentId?: string }>();
  const [searchParams] = useSearchParams();
  const appointmentId = routeId ?? searchParams.get('appointmentId') ?? 'apt-demo';
  const client = useApiClient();

  const [context, setContext] = useState<PhysicianConsultationResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadContext = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await client.get<PhysicianConsultationResponse>(
        `/bff/physician/consultation/${appointmentId}`,
      );
      setContext(res.data ?? null);
    } catch (err) {
      setError(err instanceof Error ? err.message : t('hisUnavailable'));
    } finally {
      setLoading(false);
    }
  }, [appointmentId, client, t]);

  useEffect(() => {
    void loadContext();
  }, [loadContext]);

  if (loading) return <LoadingSpinner label={t('title')} />;
  if (error) return <ErrorMessage message={error} />;

  const patient = context?.patientContext;
  const hisUnavailable = !patient || patient.status === 'DEGRADED';

  if (hisUnavailable) {
    return (
      <PageLayout>
        <EmptyState title={t('hisUnavailable')} description={t('title')} />
        <Button onClick={() => void loadContext()}>{t('refresh')}</Button>
      </PageLayout>
    );
  }

  const medicationCount = patient.medications?.length ?? 0;
  const allergyCount = patient.allergies?.length ?? 0;

  return (
    <PageLayout>
      <Stack>
        <Inline>
          <h2>{t('title')}</h2>
          <Button variant="secondary" onClick={() => void loadContext()}>
            {t('refresh')}
          </Button>
        </Inline>

        <div
          style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 16 }}
        >
          <div data-phi="true">
            <KpiCard label={t('demographics')} value={patient.name ?? '—'} />
            {patient.dateOfBirth && (
              <p style={{ margin: '4px 0 0', fontSize: '0.875rem' }} data-phi="true">
                DOB: {patient.dateOfBirth}
              </p>
            )}
          </div>
          <div data-phi="true">
            <KpiCard label={t('insurance')} value={patient.source ?? '—'} />
          </div>
          <div data-phi="true">
            <KpiCard
              label={t('medications')}
              value={medicationCount > 0 ? patient.medications!.join(', ') : '0'}
            />
          </div>
          <div data-phi="true">
            <KpiCard
              label={t('allergies')}
              value={allergyCount > 0 ? patient.allergies!.join(', ') : 'None recorded'}
            />
          </div>
          <div data-phi="true">
            <KpiCard
              label={t('history')}
              value={patient.clinicalHistory ?? patient.patientId ?? '—'}
            />
          </div>
        </div>
      </Stack>
    </PageLayout>
  );
}
