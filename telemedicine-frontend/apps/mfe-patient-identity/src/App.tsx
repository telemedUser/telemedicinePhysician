import { useState } from 'react';
import { Navigate } from 'react-router-dom';
import { useApiClient } from '@telemedicine/api-client';
import { useSession, useJourneyGate } from '@telemedicine/session-context';
import {
  Button,
  FormField,
  TextField,
  Select,
  PageLayout,
  ErrorMessage,
  LoadingSpinner,
  EmptyState,
} from '@telemedicine/design-system';
import { useTranslation } from '@telemedicine/i18n';

const MAX_ATTEMPTS = 3;

export default function App() {
  const { t } = useTranslation('identity');
  const client = useApiClient();
  const { setJourneyStatus, updateMetadata, state } = useSession();
  const allowed = useJourneyGate('CONSENT_GRANTED');

  const [documentType, setDocumentType] = useState('RUT');
  const [documentId, setDocumentId] = useState('');
  const [step, setStep] = useState<'form' | 'otp'>('form');
  const [otp, setOtp] = useState('');
  const [challengeId, setChallengeId] = useState<string | null>(null);
  const [attempts, setAttempts] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [locked, setLocked] = useState(false);

  if (!allowed) {
    return <EmptyState title={t('blocked.title')} description={t('blocked.description')} />;
  }

  if (state.journeyStatus === 'IDENTITY_VALIDATED') {
    return <Navigate to="/patient/waiting-room" replace />;
  }

  const startValidation = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await client.post<{ challengeId: string }>('/v1/identity/validate', {
        documentType,
        documentId,
        appointmentId: state.appointmentId ?? 'apt-demo',
      });
      setChallengeId(res.data?.challengeId ?? 'challenge-demo');
      setStep('otp');
    } catch (err) {
      setError(err instanceof Error ? err.message : t('errors.validate'));
    } finally {
      setLoading(false);
    }
  };

  const verifyOtp = async () => {
    if (attempts >= MAX_ATTEMPTS) {
      setLocked(true);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const res = await client.post<{ patientName?: string; insurancePlan?: string }>(
        '/v1/identity/verify',
        { challengeId, otp, appointmentId: state.appointmentId ?? 'apt-demo' },
      );
      updateMetadata({
        patientName: res.data?.patientName,
        insurancePlan: res.data?.insurancePlan,
      });
      setJourneyStatus('IDENTITY_VALIDATED');
    } catch (err) {
      setAttempts((a) => a + 1);
      setError(err instanceof Error ? err.message : t('errors.otp'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <PageLayout>
      <h2>{t('title')}</h2>
      {step === 'form' ? (
        <>
          <FormField label={t('documentType')}>
            <Select value={documentType} onChange={(e) => setDocumentType(e.target.value)}>
              <option value="RUT">{t('rut')}</option>
              <option value="PASSPORT">{t('passport')}</option>
            </Select>
          </FormField>
          <FormField label={t('documentId')}>
            <TextField value={documentId} onChange={(e) => setDocumentId(e.target.value)} />
          </FormField>
          {error && <ErrorMessage message={error} />}
          <Button disabled={!documentId || loading} onClick={() => void startValidation()}>
            {loading ? t('validating') : t('validate')}
          </Button>
        </>
      ) : (
        <>
          <p>{t('otpInstructions')}</p>
          <FormField label={t('otp')}>
            <TextField value={otp} onChange={(e) => setOtp(e.target.value)} inputMode="numeric" />
          </FormField>
          <p>{t('attempts', { count: attempts, max: MAX_ATTEMPTS })}</p>
          {locked && <ErrorMessage message={t('locked')} />}
          {error && !locked && <ErrorMessage message={error} />}
          <Button disabled={!otp || loading || locked} onClick={() => void verifyOtp()}>
            {loading ? <LoadingSpinner /> : t('verify')}
          </Button>
        </>
      )}
    </PageLayout>
  );
}
