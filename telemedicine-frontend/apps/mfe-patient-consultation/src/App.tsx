import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useVideoSession } from '@telemedicine/video-adapter';
import { useChat } from '@telemedicine/chat-adapter';
import {
  TelemetryConsentGate,
  TelemetryConfigGate,
  TelemetryEmbed,
} from '@telemedicine/telemetry-adapter';
import { useFeatureFlag } from '@telemedicine/feature-flags';
import { useSession, useJourneyGate } from '@telemedicine/session-context';
import { ConfigKeys } from '@telemedicine/shared-types';
import {
  Button,
  PageLayout,
  SplitPanel,
  StatusBadge,
  TimerDisplay,
  TextField,
  EmptyState,
  ErrorMessage,
  type BadgeVariant,
} from '@telemedicine/design-system';
import { useTranslation } from '@telemedicine/i18n';

function networkVariant(quality: number): BadgeVariant {
  if (quality >= 4) return 'ok';
  if (quality >= 2) return 'pending';
  return 'degraded';
}

export default function App() {
  const { t } = useTranslation('consultation');
  const navigate = useNavigate();
  const { state, setJourneyStatus, setJourneyStep } = useSession();
  const allowed = useJourneyGate('OPEN_DOCTOR');
  const { enabled: telemetryEnabled, loading: telemetryLoading } = useFeatureFlag(
    ConfigKeys.TELEMETRY_ENABLED,
    false,
  );

  const appointmentId = state.appointmentId ?? 'apt-demo';
  const video = useVideoSession(appointmentId, { role: 'patient', autoJoin: allowed });
  const chat = useChat({
    sessionId: video.sessionId ?? appointmentId,
    autoConnect: allowed && Boolean(video.sessionId ?? appointmentId),
  });

  const [chatInput, setChatInput] = useState('');
  const [elapsed, setElapsed] = useState(0);
  const connectedAt = useRef<number | null>(null);

  useEffect(() => {
    if (video.isConnected && connectedAt.current === null) {
      connectedAt.current = Date.now();
    }
    if (!video.isConnected) {
      connectedAt.current = null;
      setElapsed(0);
      return;
    }
    const timer = window.setInterval(() => {
      if (connectedAt.current) {
        setElapsed(Math.floor((Date.now() - connectedAt.current) / 1000));
      }
    }, 1000);
    return () => window.clearInterval(timer);
  }, [video.isConnected]);

  if (!allowed) {
    return <EmptyState title={t('blocked.title')} description={t('blocked.description')} />;
  }

  const leaveConsultation = async () => {
    await video.leave();
    await chat.disconnect();
    setJourneyStatus('CONSULTATION_CLOSED');
    setJourneyStep('documents');
    navigate('/patient/documents');
  };

  const sendChat = async () => {
    if (!chatInput.trim()) return;
    await chat.sendMessage(chatInput.trim());
    setChatInput('');
  };

  const clinicalConsentGranted =
    state.metadata.clinicalConsentGranted === true || state.journeyStatus !== 'LINK_GENERATED';

  const videoPanel = (
    <section aria-label={t('video')}>
      <h3>{t('video')}</h3>
      <div style={{ marginBottom: '0.75rem' }}>
        <StatusBadge variant={video.isConnected ? 'ok' : 'pending'}>
          {video.isConnected ? t('video') : t('reconnecting')}
        </StatusBadge>
        <StatusBadge variant={networkVariant(video.networkQuality)}>
          {t('networkQuality', { quality: video.networkQuality })}
        </StatusBadge>
      </div>
      <div
        style={{
          minHeight: 240,
          background: '#111827',
          borderRadius: 8,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: '#fff',
          marginBottom: '1rem',
        }}
      >
        {video.localStream ? (
          <video
            autoPlay
            muted
            playsInline
            ref={(el) => {
              if (el && video.localStream) el.srcObject = video.localStream;
            }}
            style={{ width: '100%', maxHeight: 320, borderRadius: 8 }}
          />
        ) : (
          <span>{video.isConnected ? t('video') : t('reconnecting')}</span>
        )}
      </div>
      <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap', marginBottom: '1rem' }}>
        <Button variant="secondary" onClick={() => void video.toggleAudio()}>
          {video.isMuted.audio ? t('unmuteAudio') : t('muteAudio')}
        </Button>
        <Button variant="secondary" onClick={() => void video.toggleVideo()}>
          {video.isMuted.video ? t('unmuteVideo') : t('muteVideo')}
        </Button>
      </div>
      <p>
        {t('timer')}: <TimerDisplay seconds={elapsed} />
      </p>
      {video.error && <ErrorMessage message={video.error.message} />}
      {!telemetryLoading && telemetryEnabled && (
        <section style={{ marginTop: '1.5rem' }}>
          <h3>{t('telemetry')}</h3>
          <TelemetryConsentGate consentGranted={clinicalConsentGranted}>
            <TelemetryConfigGate>
              <TelemetryEmbed appointmentId={appointmentId} consentGranted={clinicalConsentGranted} />
            </TelemetryConfigGate>
          </TelemetryConsentGate>
        </section>
      )}
    </section>
  );

  const chatPanel = (
    <section aria-label={t('chat')}>
      <h3>{t('chat')}</h3>
      <div
        role="log"
        aria-live="polite"
        style={{
          minHeight: 240,
          maxHeight: 320,
          overflowY: 'auto',
          border: '1px solid var(--tm-color-border)',
          borderRadius: 8,
          padding: 12,
          marginBottom: '1rem',
        }}
      >
        {chat.messages.length === 0 && <p style={{ color: 'var(--tm-color-text-muted)' }}>{t('chat')}</p>}
        {chat.messages.map((msg) => (
          <div key={msg.id} style={{ marginBottom: 8 }}>
            <strong>{msg.senderRole}: </strong>
            {msg.body}
          </div>
        ))}
        {chat.typing && <p>{t('reconnecting')}</p>}
      </div>
      <div style={{ display: 'flex', gap: 8 }}>
        <TextField
          value={chatInput}
          onChange={(e) => setChatInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') void sendChat();
          }}
          aria-label={t('chat')}
        />
        <Button onClick={() => void sendChat()} disabled={!chatInput.trim()}>
          {t('chat')}
        </Button>
      </div>
      {chat.error && <ErrorMessage message={chat.error.message} />}
    </section>
  );

  return (
    <PageLayout>
      <h2>{t('title')}</h2>
      <SplitPanel left={videoPanel} right={chatPanel} />
      <div style={{ marginTop: '1.5rem' }}>
        <Button variant="secondary" onClick={() => void leaveConsultation()}>
          {t('leave')}
        </Button>
      </div>
    </PageLayout>
  );
}
