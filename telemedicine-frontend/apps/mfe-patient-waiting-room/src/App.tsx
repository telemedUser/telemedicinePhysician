import { useCallback, useEffect, useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { useApiClient } from '@telemedicine/api-client';
import { DeviceTestPanel } from '@telemedicine/device-test';
import { useSession, useJourneyGate } from '@telemedicine/session-context';
import {
  Button,
  PageLayout,
  StatusBadge,
  EmptyState,
  ErrorMessage,
  LoadingSpinner,
  type BadgeVariant,
} from '@telemedicine/design-system';
import { useTranslation } from '@telemedicine/i18n';
import { hasReachedStatus } from '@telemedicine/shared-types';

const POLL_INTERVAL_MS = 5000;

interface QueuePosition {
  appointmentId: string;
  position: number;
  estimatedWaitSec: number;
  event?: string;
}

interface VideoSessionSummary {
  sessionId?: string;
  status?: string;
}

function isPaymentPending(metadata: Record<string, unknown>): boolean {
  if (metadata.paymentPending === true) return true;
  const rootStatus = metadata.paymentStatus as string | undefined;
  if (rootStatus && !['CONFIRMED', 'PAID'].includes(rootStatus.toUpperCase())) return true;
  const payment = metadata.payment as { status?: string; paid?: boolean } | undefined;
  if (payment?.paid === false) return true;
  if (payment?.status && !['CONFIRMED', 'PAID'].includes(payment.status.toUpperCase())) return true;
  const appointment = metadata.appointment as { paymentStatus?: string } | undefined;
  const status = appointment?.paymentStatus;
  return Boolean(status && !['CONFIRMED', 'PAID'].includes(status.toUpperCase()));
}

function queueVariant(position: number): BadgeVariant {
  if (position <= 1) return 'ok';
  if (position <= 3) return 'pending';
  return 'info';
}

export default function App() {
  const { t } = useTranslation('waitingRoom');
  const client = useApiClient();
  const navigate = useNavigate();
  const { state, setJourneyStatus, setJourneyStep } = useSession();
  const allowed = useJourneyGate('IDENTITY_VALIDATED');

  const appointmentId = state.appointmentId ?? 'apt-demo';
  const paymentPending = isPaymentPending(state.metadata);

  const [queue, setQueue] = useState<QueuePosition | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const inConsultation = state.journeyStatus === 'IN_CONSULTATION';
  const physicianReady = hasReachedStatus(state.journeyStatus, 'OPEN_DOCTOR');

  const checkPhysicianReady = useCallback(
    async (currentAppointmentId: string) => {
      try {
        const videoRes = await client.get<VideoSessionSummary>(
          `/v1/video/appointments/${currentAppointmentId}/session`,
        );
        const status = videoRes.data?.status?.toUpperCase();
        if (videoRes.data?.sessionId && status && status !== 'ENDED') {
          setJourneyStatus('OPEN_DOCTOR');
          setJourneyStep('consultation');
        }
      } catch {
        /* video session may not exist yet */
      }
    },
    [client, setJourneyStatus, setJourneyStep],
  );

  const pollQueue = useCallback(async () => {
    if (paymentPending || inConsultation) return;
    try {
      const res = await client.get<QueuePosition>(`/v1/waiting-room/queue/${appointmentId}`);
      if (res.data) {
        setQueue(res.data);
        setError(null);
      }
      await checkPhysicianReady(appointmentId);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Queue unavailable');
    } finally {
      setLoading(false);
    }
  }, [appointmentId, checkPhysicianReady, client, inConsultation, paymentPending]);

  useEffect(() => {
    if (!allowed || paymentPending) {
      setLoading(false);
      return;
    }
    void pollQueue();
    const timer = window.setInterval(() => void pollQueue(), POLL_INTERVAL_MS);
    return () => window.clearInterval(timer);
  }, [allowed, paymentPending, pollQueue]);

  if (!allowed) {
    return <EmptyState title={t('blocked.title')} description={t('blocked.description')} />;
  }

  if (physicianReady && state.journeyStatus === 'OPEN_DOCTOR') {
    return <Navigate to="/patient/consultation" replace />;
  }

  const joinConsultation = () => {
    setJourneyStatus('IN_CONSULTATION');
    setJourneyStep('consultation');
    navigate('/patient/consultation');
  };

  return (
    <PageLayout>
      <h2>{t('title')}</h2>

      {inConsultation && (
        <StatusBadge variant="info">{t('inConsultation')}</StatusBadge>
      )}

      {paymentPending && (
        <StatusBadge variant="pending">{t('paymentPending')}</StatusBadge>
      )}

      {!paymentPending && !inConsultation && (
        <>
          {loading && !queue && <LoadingSpinner />}
          {error && <ErrorMessage message={error} />}
          {queue && (
            <div style={{ marginBottom: '1rem' }}>
              <p>{t('queuePosition', { position: queue.position })}</p>
              <p>{t('estimatedWait', { minutes: Math.ceil(queue.estimatedWaitSec / 60) })}</p>
              <StatusBadge variant={queueVariant(queue.position)}>
                {t('queuePosition', { position: queue.position })}
              </StatusBadge>
            </div>
          )}
          <p>{t('guidedWait')}</p>
          {physicianReady && (
            <>
              <p>{t('physicianReady')}</p>
              <Button onClick={joinConsultation}>{t('joinConsultation')}</Button>
            </>
          )}
        </>
      )}

      <section style={{ marginTop: '2rem' }}>
        <h3>{t('deviceTest')}</h3>
        <DeviceTestPanel appointmentId={appointmentId} />
      </section>
    </PageLayout>
  );
}
