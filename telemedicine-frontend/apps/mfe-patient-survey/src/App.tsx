import { useCallback, useEffect, useMemo, useState } from 'react';
import { Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { useApiClient } from '@telemedicine/api-client';
import { useSession, useJourneyGate } from '@telemedicine/session-context';
import {
  Button,
  PageLayout,
  FormField,
  TextArea,
  EmptyState,
  ErrorMessage,
  LoadingSpinner,
} from '@telemedicine/design-system';
import { useTranslation } from '@telemedicine/i18n';

interface SurveyQuestion {
  id: string;
  type: 'nps' | 'text' | 'rating';
  prompt: string;
  required?: boolean;
}

interface SurveyDefinition {
  appointmentId: string;
  available: boolean;
  submitted: boolean;
  questions?: SurveyQuestion[];
}

const DEFAULT_QUESTIONS = (npsLabel: string): SurveyQuestion[] => [
  { id: 'nps', type: 'nps', prompt: npsLabel, required: true },
];

function JourneyBlocked() {
  const { t } = useTranslation('survey');
  return <EmptyState title={t('blocked.title')} description={t('blocked.description')} />;
}

function ThankYouPage() {
  const { t } = useTranslation('survey');
  const navigate = useNavigate();

  return (
    <PageLayout>
      <h2>{t('thankYou')}</h2>
      <p>{t('thankYouHint')}</p>
      <Button variant="secondary" onClick={() => navigate('/patient/documents')}>
        {t('viewDocuments')}
      </Button>
    </PageLayout>
  );
}

function SurveyFormPage({ onComplete }: { onComplete: () => void }) {
  const { t } = useTranslation('survey');
  const client = useApiClient();
  const navigate = useNavigate();
  const location = useLocation();
  const { state, setJourneyStatus, setJourneyStep } = useSession();

  const appointmentId = state.appointmentId ?? 'apt-demo';
  const [survey, setSurvey] = useState<SurveyDefinition | null>(null);
  const [answers, setAnswers] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchSurvey = useCallback(async () => {
    setLoading(true);
    try {
      const res = await client.get<SurveyDefinition>(`/v1/survey/${appointmentId}`);
      if (res.data) {
        setSurvey(res.data);
        if (res.data.submitted) {
          onComplete();
          navigate(`${location.pathname.replace(/\/$/, '')}/thank-you`, { replace: true });
        }
      }
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Survey unavailable');
    } finally {
      setLoading(false);
    }
  }, [appointmentId, client, location.pathname, navigate, onComplete]);

  useEffect(() => {
    void fetchSurvey();
  }, [fetchSurvey]);

  const questions = useMemo(() => {
    if (survey?.questions?.length) return survey.questions;
    return DEFAULT_QUESTIONS(t('npsLabel'));
  }, [survey?.questions, t]);

  const npsValue = Number(answers.nps ?? '');
  const canSubmit = questions.every((q) => {
    if (!q.required) return true;
    const value = answers[q.id];
    return value !== undefined && value !== '';
  });

  const submitSurvey = async () => {
    setSubmitting(true);
    setError(null);
    try {
      const comment = Object.entries(answers)
        .filter(([id]) => id !== 'nps')
        .map(([, value]) => value)
        .join('\n')
        .trim();

      await client.post(
        `/v1/survey/${appointmentId}/responses`,
        { nps: npsValue, comment: comment || undefined },
        { idempotencyKey: `survey-${appointmentId}` },
      );
      setJourneyStatus('SURVEY_COMPLETED');
      setJourneyStep('complete');
      onComplete();
      navigate(`${location.pathname.replace(/\/$/, '')}/thank-you`, { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Submit failed');
    } finally {
      setSubmitting(false);
    }
  };

  const skipSurvey = () => {
    setJourneyStep('complete');
    onComplete();
    navigate(`${location.pathname.replace(/\/$/, '')}/thank-you`);
  };

  if (loading) {
    return (
      <PageLayout>
        <LoadingSpinner />
      </PageLayout>
    );
  }

  if (survey && !survey.available && !survey.submitted) {
    return <EmptyState title={t('blocked.title')} description={t('blocked.description')} />;
  }

  return (
    <PageLayout>
      <h2>{t('title')}</h2>
      {error && <ErrorMessage message={error} />}
      <form
        onSubmit={(e) => {
          e.preventDefault();
          void submitSurvey();
        }}
      >
        {questions.map((question) => (
          <div key={question.id} style={{ marginBottom: '1.5rem' }}>
            {question.type === 'nps' ? (
              <FormField label={question.prompt || t('npsLabel')}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                  <span>{t('npsLow')}</span>
                  <span>{t('npsHigh')}</span>
                </div>
                <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
                  {Array.from({ length: 11 }, (_, score) => (
                    <label key={score} style={{ cursor: 'pointer' }}>
                      <input
                        type="radio"
                        name="nps"
                        value={score}
                        checked={answers.nps === String(score)}
                        onChange={() => setAnswers((prev) => ({ ...prev, nps: String(score) }))}
                      />
                      {score}
                    </label>
                  ))}
                </div>
              </FormField>
            ) : (
              <FormField label={question.prompt}>
                <TextArea
                  value={answers[question.id] ?? ''}
                  onChange={(e) =>
                    setAnswers((prev) => ({ ...prev, [question.id]: e.target.value }))
                  }
                />
              </FormField>
            )}
          </div>
        ))}
        <div style={{ display: 'flex', gap: '0.75rem' }}>
          <Button type="submit" disabled={!canSubmit || submitting || Number.isNaN(npsValue)}>
            {submitting ? <LoadingSpinner /> : t('submit')}
          </Button>
          <Button type="button" variant="secondary" onClick={skipSurvey}>
            {t('skip')}
          </Button>
        </div>
      </form>
    </PageLayout>
  );
}

export default function App() {
  const allowed = useJourneyGate('CONSULTATION_CLOSED');
  const location = useLocation();
  const [completed, setCompleted] = useState(location.pathname.endsWith('/thank-you'));

  if (!allowed) {
    return <JourneyBlocked />;
  }

  if (completed || location.pathname.endsWith('/thank-you')) {
    return <ThankYouPage />;
  }

  return (
    <Routes>
      <Route path="/" element={<SurveyFormPage onComplete={() => setCompleted(true)} />} />
      <Route path="/thank-you" element={<ThankYouPage />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
