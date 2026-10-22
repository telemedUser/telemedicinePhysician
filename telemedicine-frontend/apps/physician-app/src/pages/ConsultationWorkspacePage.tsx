import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { labels } from '../config/labels';
import { Avatar, BpButton, BpInput, Card, Icon, Overline, Pill } from '../ui/primitives';
import { CONSULTATION_FLOW_POINTS, MOCK_CONTEXTS, MOCK_TRANSCRIPT, MOCK_VITALS } from '../data/mockData';
import { VideoCall } from '../video/VideoCall';
import { resolveVideoConfig } from '../video/videoConfig';

type VitalsState = 'idle' | 'running' | 'done' | 'failed';
type AiState = 'off' | 'physician' | 'patient_pending' | 'patient_declined' | 'on';
type TxState = 'off' | 'running' | 'stopped';

function Vital({ icon, label, value, unit }: { icon: string; label: string; value: string | number; unit?: string }) {
  return (
    <div style={{ textAlign: 'center', padding: '12px 8px', background: 'var(--bp-surface-2)', borderRadius: 8 }}>
      <Icon name={icon} size={20} color="var(--bp-blue)" />
      <div style={{ fontSize: 20, fontWeight: 700, marginTop: 4 }}>
        {value}
        {unit && <span style={{ fontSize: 12, color: 'var(--bp-muted)', marginLeft: 2 }}>{unit}</span>}
      </div>
      <div style={{ fontSize: 11, color: 'var(--bp-muted)' }}>{label}</div>
    </div>
  );
}

export function ConsultationWorkspacePage() {
  const { appointmentId = '' } = useParams<{ appointmentId: string }>();
  const navigate = useNavigate();
  const ctx = MOCK_CONTEXTS[appointmentId];
  const isAudio = ctx?.name.includes('Lakshmi');

  const [messages, setMessages] = useState(MOCK_TRANSCRIPT);
  const [draft, setDraft] = useState('');
  const [elapsed, setElapsed] = useState(0);
  const [vitals, setVitals] = useState<VitalsState>('idle');
  const [ai, setAi] = useState<AiState>('off');
  const [tx, setTx] = useState<TxState>('off');
  const txRef = useRef<number | null>(null);

  useEffect(() => {
    const id = window.setInterval(() => setElapsed((e) => e + 1), 1000);
    return () => window.clearInterval(id);
  }, []);

  const send = () => {
    const text = draft.trim();
    if (!text) return;
    const at = new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
    setMessages((prev) => [...prev, { from: 'physician', text, at }]);
    setDraft('');
  };

  const runVitals = () => {
    setVitals('running');
    window.setTimeout(() => setVitals('done'), 2200);
  };

  // AI two-consent flow (deck D-06): physician consents, then patient on their own screen.
  const consentPhysician = () => {
    setAi('patient_pending');
    window.setTimeout(() => setAi('on'), 2600);
  };

  const toggleTranscript = () => {
    if (tx === 'running') {
      setTx('stopped');
      if (txRef.current) window.clearInterval(txRef.current);
    } else {
      setTx('running');
      txRef.current = window.setInterval(() => {
        const at = new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
        setMessages((prev) => [...prev, { from: 'patient', text: '…(transcript) I still have mild discomfort in the mornings.', at }]);
      }, 6000);
    }
  };
  useEffect(() => () => { if (txRef.current) window.clearInterval(txRef.current); }, []);

  const mm = String(Math.floor(elapsed / 60)).padStart(2, '0');
  const ss = String(elapsed % 60).padStart(2, '0');
  const patientName = ctx?.name ?? appointmentId;

  // Live video: the physician joins the room keyed by the appointment reference as the "doctor"
  // peer; the patient app joins the same room. ICE (STUN/TURN) makes it work across networks.
  const video = resolveVideoConfig();

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      {/* Header strip */}
      <Card style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
        <Avatar initials="PT" size={44} tone="navy" />
        <div style={{ flex: 1, minWidth: 0 }}>
          <strong style={{ fontSize: 16 }} data-phi="true">{patientName}</strong>
          <div style={{ display: 'flex', gap: 8, marginTop: 4, flexWrap: 'wrap' }}>
            <Pill tone="green" dot>In consultation</Pill>
            <Pill tone="neutral" icon={isAudio ? 'call' : 'videocam'}>{isAudio ? 'Audio' : 'Video'}</Pill>
            <Pill tone="neutral" icon="schedule">{mm}:{ss}</Pill>
            <span style={{ color: 'var(--bp-muted-2)', fontSize: 12, fontFamily: 'ui-monospace, Menlo, monospace' }}>{appointmentId}</span>
          </div>
        </div>
        <Link to={`/worklist/${appointmentId}/context`}>
          <BpButton variant="ghost" icon="person">Context</BpButton>
        </Link>
        <Link to={`/consultation/${appointmentId}/documentation`}>
          <BpButton variant="secondary" icon="edit_document">{labels.documentation.title}</BpButton>
        </Link>
        <BpButton variant="ghost" icon="gavel" onClick={() => navigate(`/consultation/${appointmentId}/withdrawn`)}>
          Consent withdrawn
        </BpButton>
        <BpButton variant="danger" icon="call_end" onClick={() => navigate(`/consultation/${appointmentId}/review`)}>
          End & review
        </BpButton>
      </Card>

      <div className="grid-2col" style={{ display: 'grid', gridTemplateColumns: '1.35fr 1fr', gap: 16, alignItems: 'start' }}>
        {/* LEFT: live video call + chat/transcript */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <Card padded={false} style={{ padding: 12 }}>
            <VideoCall
              roomId={appointmentId}
              role="doctor"
              wsUrl={video.wsUrl}
              iceServers={video.iceServers}
              onEnded={() => navigate(`/consultation/${appointmentId}/review`)}
            />
          </Card>

          <Card padded={false} style={{ display: 'flex', flexDirection: 'column', height: 360 }}>
            <div style={{ padding: '12px 16px', borderBottom: '1px solid var(--bp-border-2)', display: 'flex', alignItems: 'center', gap: 8 }}>
              <Icon name="forum" size={20} color="var(--bp-blue)" />
              <strong>{labels.workspace.chat}</strong>
              <Pill tone="green" dot>Connected</Pill>
              {tx !== 'off' && (
                <span style={{ marginLeft: 'auto' }}>
                  <Pill tone={tx === 'running' ? 'red' : 'amber'} dot>
                    {tx === 'running' ? labels.transcript.running : labels.transcript.stopped}
                  </Pill>
                </span>
              )}
            </div>
            <div style={{ flex: 1, overflow: 'auto', padding: 16, display: 'flex', flexDirection: 'column', gap: 10 }}>
              {/* Guided end-to-end flow points (what the physician can do in this service) */}
              <div
                style={{
                  background: 'var(--bp-blue-tint-2)',
                  border: '1px solid var(--bp-border-2)',
                  borderRadius: 10,
                  padding: '10px 12px',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 6 }}>
                  <Icon name="route" size={16} color="var(--bp-blue-dark)" />
                  <strong style={{ fontSize: 12, color: 'var(--bp-blue-dark)' }}>
                    Consultation flow — what you can do
                  </strong>
                </div>
                <ol style={{ margin: 0, paddingLeft: 18, fontSize: 12, color: 'var(--bp-text-2)', lineHeight: 1.6 }}>
                  {CONSULTATION_FLOW_POINTS.map((p) => (
                    <li key={p.step}>{p.text}</li>
                  ))}
                </ol>
              </div>

              {messages.map((m, i) => {
                const own = m.from === 'physician';
                return (
                  <div key={i} style={{ display: 'flex', justifyContent: own ? 'flex-end' : 'flex-start', gap: 8 }}>
                    {!own && <Avatar initials="PT" size={26} tone="navy" />}
                    <div style={{ maxWidth: '76%', padding: '8px 12px', borderRadius: 12, background: own ? 'var(--bp-blue)' : 'var(--bp-surface-2)', color: own ? '#fff' : 'var(--bp-text)', fontSize: 14, lineHeight: 1.45 }}>
                      <span data-phi="true">{m.text}</span>
                      <div style={{ fontSize: 10, opacity: 0.7, marginTop: 3, textAlign: 'right' }}>{m.at}</div>
                    </div>
                  </div>
                );
              })}
            </div>
            <div style={{ padding: 12, borderTop: '1px solid var(--bp-border-2)', display: 'flex', gap: 8 }}>
              <BpInput value={draft} onChange={(e) => setDraft(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && send()} placeholder="Type a clinical message…" />
              <BpButton icon="send" onClick={send}>Send</BpButton>
            </div>
          </Card>
        </div>

        {/* RIGHT: vitals, AI assistant, transcript */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          {/* Vitals (D-05) */}
          <Card>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
              <Icon name="monitor_heart" size={20} color="var(--bp-blue)" />
              <strong>{labels.workspace.telemetry}</strong>
              <span style={{ marginLeft: 'auto' }}>
                {vitals === 'done' ? <Pill tone="green" icon="check_circle">Completed</Pill> : vitals === 'running' ? <Pill tone="amber" dot>Measuring…</Pill> : null}
              </span>
            </div>
            {vitals === 'done' ? (
              <>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 10 }}>
                  <Vital icon="ecg_heart" label="Heart rate" value={MOCK_VITALS.heartRate} unit="bpm" />
                  <Vital icon="spo2" label="SpO₂" value={MOCK_VITALS.spo2} unit="%" />
                  <Vital icon="pulmonology" label="Respiratory rate" value={MOCK_VITALS.respiratoryRate} unit="rpm" />
                  <Vital icon="self_improvement" label="Stress" value={MOCK_VITALS.stress} />
                </div>
                <div style={{ marginTop: 8, fontSize: 11, color: 'var(--bp-muted)' }}>ANURA measurement · {MOCK_VITALS.measuredAt} · written into the note</div>
              </>
            ) : (
              <>
                <p style={{ margin: '0 0 10px', fontSize: 13, color: 'var(--bp-muted)' }}>
                  Camera-based vitals measurement. The patient consents on their own screen; the result is written into the note.
                </p>
                <BpButton variant="subtle" icon="camera" loading={vitals === 'running'} onClick={runVitals}>
                  Request vitals measurement
                </BpButton>
              </>
            )}
          </Card>

          {/* AI assistant two-consent (D-06) */}
          <Card>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 10 }}>
              <Icon name="neurology" size={20} color="var(--bp-blue)" />
              <strong>{labels.ai.title}</strong>
              <span style={{ marginLeft: 'auto' }}>
                {ai === 'off' && <Pill tone="neutral">{labels.ai.off}</Pill>}
                {ai === 'patient_pending' && <Pill tone="amber" dot>{labels.ai.waiting}</Pill>}
                {ai === 'on' && <Pill tone="green" dot>Active</Pill>}
                {ai === 'patient_declined' && <Pill tone="red">Declined</Pill>}
              </span>
            </div>
            <p style={{ margin: '0 0 12px', fontSize: 12, color: 'var(--bp-muted)', lineHeight: 1.5 }}>{labels.ai.disclosure}</p>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              <ConsentRow label={labels.ai.physicianConsent} done={ai !== 'off'} />
              <ConsentRow
                label={labels.ai.patientConsent}
                done={ai === 'on'}
                pending={ai === 'patient_pending'}
                declined={ai === 'patient_declined'}
              />
            </div>
            <div style={{ marginTop: 12, display: 'flex', gap: 8 }}>
              {ai === 'off' && <BpButton icon="check" onClick={consentPhysician}>{labels.ai.enable}</BpButton>}
              {ai === 'patient_pending' && (
                <BpButton variant="ghost" icon="cancel" onClick={() => setAi('patient_declined')}>Patient declines</BpButton>
              )}
            </div>
          </Card>

          {/* Live transcript (D-07) */}
          <Card>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 10 }}>
              <Icon name="record_voice_over" size={20} color="var(--bp-blue)" />
              <strong>{labels.transcript.title}</strong>
              {tx === 'running' && <span style={{ marginLeft: 'auto' }}><Pill tone="red" dot>{labels.transcript.running}</Pill></span>}
              {tx === 'stopped' && <span style={{ marginLeft: 'auto' }}><Pill tone="amber">{labels.transcript.stopped}</Pill></span>}
            </div>
            <BpButton
              variant={tx === 'running' ? 'danger' : 'subtle'}
              icon={tx === 'running' ? 'stop' : 'mic'}
              disabled={ai !== 'on' && tx === 'off'}
              onClick={toggleTranscript}
            >
              {tx === 'running' ? labels.transcript.stop : labels.transcript.start}
            </BpButton>
            {ai !== 'on' && tx === 'off' && (
              <p style={{ margin: '8px 0 0', fontSize: 12, color: 'var(--bp-muted)' }}>Requires the AI assistant active (both consents).</p>
            )}
          </Card>

          {ctx && (
            <Card>
              <Overline>Context</Overline>
              <p style={{ margin: '8px 0 0', fontSize: 14, color: 'var(--bp-text-2)', lineHeight: 1.5 }} data-phi="true">{ctx.reason}</p>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}

function ConsentRow({ label, done, pending, declined }: { label: string; done?: boolean; pending?: boolean; declined?: boolean }) {
  const icon = done ? 'check_circle' : declined ? 'cancel' : pending ? 'hourglass_top' : 'radio_button_unchecked';
  const color = done ? 'var(--bp-ok)' : declined ? 'var(--bp-danger)' : pending ? 'var(--bp-warn)' : 'var(--bp-muted-2)';
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 14 }}>
      <Icon name={icon} size={18} color={color} fill={done} />
      <span>{label}</span>
      {pending && <span style={{ marginLeft: 'auto', fontSize: 12, color: 'var(--bp-warn)' }}>{labels.ai.patientPending}</span>}
      {declined && <span style={{ marginLeft: 'auto', fontSize: 12, color: 'var(--bp-danger)' }}>{labels.ai.patientDeclined}</span>}
    </div>
  );
}
