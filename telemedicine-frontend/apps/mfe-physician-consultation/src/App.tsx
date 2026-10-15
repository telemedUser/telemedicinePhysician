import { useCallback, useEffect, useState } from 'react';
import { Link, Navigate, Route, Routes, useNavigate, useParams } from 'react-router-dom';
import { useApiClient } from '@telemedicine/api-client';
import { ChatPanel, useChat } from '@telemedicine/chat-adapter';
import {
  Button,
  EmptyState,
  ErrorMessage,
  Inline,
  LoadingSpinner,
  PageLayout,
  SplitPanel,
  Stack,
  StatusBadge,
} from '@telemedicine/design-system';
import { useTranslation } from '@telemedicine/i18n';
import type {
  PhysicianConsultationResponse,
  PhysicianWorklistResponse,
} from '@telemedicine/shared-types';
import { TelemetryResults, useTelemetryAdapter } from '@telemedicine/telemetry-adapter';
import { useVideoSession } from '@telemedicine/video-adapter';

function WorklistPage() {
  const { t } = useTranslation('physicianConsultation');
  const client = useApiClient();
  const navigate = useNavigate();
  const [worklist, setWorklist] = useState<PhysicianWorklistResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [admitting, setAdmitting] = useState<string | null>(null);

  const loadWorklist = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await client.get<PhysicianWorklistResponse>('/bff/physician/worklist');
      setWorklist(res.data ?? { status: 'OK', entries: [] });
    } catch (err) {
      setError(err instanceof Error ? err.message : t('blocked'));
    } finally {
      setLoading(false);
    }
  }, [client, t]);

  useEffect(() => {
    void loadWorklist();
  }, [loadWorklist]);

  const admitPatient = async (appointmentId: string) => {
    setAdmitting(appointmentId);
    try {
      await client.put(`/v1/waiting-room/status/${appointmentId}`, { status: 'OPEN_DOCTOR' });
      navigate(`/physician/consultation/${appointmentId}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : t('blocked'));
    } finally {
      setAdmitting(null);
    }
  };

  if (loading) return <LoadingSpinner label={t('worklist')} />;
  if (error) return <ErrorMessage message={error} />;
  if (!worklist?.entries.length) {
    return <EmptyState title={t('blocked')} description={t('worklist')} />;
  }

  return (
    <PageLayout>
      <Stack>
        <h2>{t('worklist')}</h2>
        <Stack gap={12}>
          {worklist.entries.map((entry) => (
            <Inline key={entry.appointmentId}>
              <StatusBadge variant={entry.status === 'CHECKED_IN' ? 'pending' : 'ok'}>
                {entry.status}
              </StatusBadge>
              <span>{entry.appointmentId}</span>
              <span data-phi="true">{entry.patientId ?? entry.patientName ?? '—'}</span>
              <Button
                loading={admitting === entry.appointmentId}
                disabled={entry.status !== 'CHECKED_IN' && entry.status !== 'UNKNOWN'}
                onClick={() => void admitPatient(entry.appointmentId)}
              >
                {t('admit')}
              </Button>
              <Link to={`/${entry.appointmentId}`}>
                <Button variant="secondary">{t('join')}</Button>
              </Link>
            </Inline>
          ))}
        </Stack>
      </Stack>
    </PageLayout>
  );
}

function VideoPanel({ appointmentId }: { appointmentId: string }) {
  const { t } = useTranslation('physicianConsultation');
  const video = useVideoSession(appointmentId, { role: 'physician', autoJoin: true });

  return (
    <Stack>
      <Inline>
        <StatusBadge variant={video.isConnected ? 'ok' : 'pending'}>
          {video.isConnected ? 'Connected' : 'Connecting'}
        </StatusBadge>
        <span>Network: {video.networkQuality}/5</span>
      </Inline>
      <div
        aria-label="Physician video"
        style={{
          minHeight: 240,
          background: '#111827',
          borderRadius: 8,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: '#fff',
        }}
      >
        {video.localStream ? (
          <video
            autoPlay
            muted
            playsInline
            ref={(node) => {
              if (node && video.localStream) node.srcObject = video.localStream;
            }}
            style={{ width: '100%', maxHeight: 320, borderRadius: 8 }}
          />
        ) : (
          <span>Video session {video.sessionId ?? appointmentId}</span>
        )}
      </div>
      {video.error && <ErrorMessage message={video.error.message} />}
      <Inline>
        <Button variant="secondary" onClick={() => void video.toggleAudio()}>
          {video.isMuted.audio ? 'Unmute' : 'Mute'}
        </Button>
        <Button variant="secondary" onClick={() => void video.toggleVideo()}>
          {video.isMuted.video ? 'Camera on' : 'Camera off'}
        </Button>
        <Button variant="danger" onClick={() => void video.leave()}>
          {t('endConsultation')}
        </Button>
      </Inline>
    </Stack>
  );
}

function ConsultationSidePanel({
  appointmentId,
  chatSessionId,
  telemetryEnabled,
}: {
  appointmentId: string;
  chatSessionId: string;
  telemetryEnabled: boolean;
}) {
  const chat = useChat({ sessionId: chatSessionId, autoConnect: true });
  const telemetryAdapter = useTelemetryAdapter();
  const [telemetryResults, setTelemetryResults] = useState(
    telemetryAdapter.getResults(),
  );

  useEffect(() => {
    if (!telemetryEnabled) return;
    let active = true;
    void (async () => {
      await telemetryAdapter.initialize({ participantId: appointmentId });
      await telemetryAdapter.startMeasurement(appointmentId);
    })();
    const unsub = telemetryAdapter.onMeasurementComplete((results) => {
      if (active) setTelemetryResults(results);
    });
    const poll = window.setInterval(() => {
      const latest = telemetryAdapter.getResults();
      if (latest) setTelemetryResults(latest);
    }, 2000);
    return () => {
      active = false;
      unsub();
      window.clearInterval(poll);
      telemetryAdapter.destroy();
    };
  }, [appointmentId, telemetryAdapter, telemetryEnabled]);

  return (
    <Stack>
      <section aria-label="Clinical chat panel" style={{ minHeight: 280 }}>
        <ChatPanel
          messages={chat.messages}
          typingVisible={chat.typing}
          onSend={(text) => void chat.sendMessage(text, true)}
          onTyping={chat.setTyping}
          disabled={!chat.connected}
        />
      </section>
      <section aria-label="Patient telemetry" data-readonly="true">
        <TelemetryResults results={telemetryResults} />
      </section>
    </Stack>
  );
}

function ConsultationPage() {
  const { t } = useTranslation('physicianConsultation');
  const { appointmentId } = useParams<{ appointmentId: string }>();
  const client = useApiClient();
  const navigate = useNavigate();
  const [context, setContext] = useState<PhysicianConsultationResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [ending, setEnding] = useState(false);

  useEffect(() => {
    if (!appointmentId) return;
    let active = true;
    void (async () => {
      setLoading(true);
      setError(null);
      try {
        const res = await client.get<PhysicianConsultationResponse>(
          `/bff/physician/consultation/${appointmentId}`,
        );
        if (active) setContext(res.data ?? null);
      } catch (err) {
        if (active) setError(err instanceof Error ? err.message : t('blocked'));
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => {
      active = false;
    };
  }, [appointmentId, client, t]);

  const endConsultation = async () => {
    if (!appointmentId) return;
    setEnding(true);
    try {
      await client.put(`/v1/waiting-room/status/${appointmentId}`, { status: 'COMPLETED' });
      navigate('/physician/consultation');
    } catch (err) {
      setError(err instanceof Error ? err.message : t('blocked'));
    } finally {
      setEnding(false);
    }
  };

  if (!appointmentId) return <Navigate to="/" replace />;
  if (loading) return <LoadingSpinner label={t('join')} />;
  if (error) return <ErrorMessage message={error} />;
  if (!context) return <EmptyState title={t('blocked')} />;

  const chatSessionId = context.chat.threadId ?? context.video.sessionId ?? appointmentId;

  return (
    <PageLayout>
      <Stack>
        <Inline>
          <h2>{t('join')}</h2>
          <StatusBadge variant={context.status === 'OK' ? 'ok' : 'degraded'}>
            {context.status}
          </StatusBadge>
          <span data-phi="true">{context.patientContext.name ?? context.patientContext.patientId}</span>
        </Inline>
        <SplitPanel
          left={<VideoPanel appointmentId={appointmentId} />}
          right={
            <ConsultationSidePanel
              appointmentId={appointmentId}
              chatSessionId={chatSessionId}
              telemetryEnabled={context.telemetry.enabled ?? false}
            />
          }
        />
        <Inline>
          <Button variant="danger" loading={ending} onClick={() => void endConsultation()}>
            {t('endConsultation')}
          </Button>
          <Link to="/physician/consultation">
            <Button variant="ghost">{t('worklist')}</Button>
          </Link>
        </Inline>
      </Stack>
    </PageLayout>
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="/physician/consultation" element={<WorklistPage />} />
      <Route path="/physician/consultation/:appointmentId" element={<ConsultationPage />} />
    </Routes>
  );
}
