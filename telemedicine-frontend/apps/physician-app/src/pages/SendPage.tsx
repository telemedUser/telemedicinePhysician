import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { labels } from '../config/labels';
import { BpButton, Card, Icon, Pill, SectionHeader, type Tone } from '../ui/primitives';
import { sendDocsFor, type DocStatus, type SendableDoc } from '../data/mockData';

const STATUS_META: Record<DocStatus, { tone: Tone; icon: string; label: string }> = {
  DRAFT: { tone: 'neutral', icon: 'draft', label: labels.send.statusDRAFT },
  READY: { tone: 'blue', icon: 'schedule_send', label: labels.send.statusREADY },
  SENDING: { tone: 'amber', icon: 'sync', label: labels.send.statusSENDING },
  CONFIRMED: { tone: 'green', icon: 'check_circle', label: labels.send.statusCONFIRMED },
  QUEUED: { tone: 'amber', icon: 'hourglass_top', label: labels.send.statusQUEUED },
  ERROR: { tone: 'red', icon: 'error', label: labels.send.statusERROR },
};

// Deck D-09 exceptions: Medipass validation error (leave missing start date), ReceMed slow (queued).
function simulateOutcome(doc: SendableDoc): { status: DocStatus; detail?: string } {
  if (doc.system === 'Medipass') return { status: 'ERROR', detail: 'Medipass validation: the leave is missing a start date.' };
  if (doc.system === 'ReceMed') return { status: 'QUEUED', detail: 'ReceMed slow · prescription queued, retrying.' };
  return { status: 'CONFIRMED' };
}

export function SendPage() {
  const { appointmentId = '' } = useParams<{ appointmentId: string }>();
  const navigate = useNavigate();
  const [docs, setDocs] = useState<SendableDoc[]>(() => sendDocsFor(appointmentId));

  const update = (id: string, patch: Partial<SendableDoc>) =>
    setDocs((prev) => prev.map((d) => (d.id === id ? { ...d, ...patch } : d)));

  const sendOne = (doc: SendableDoc) => {
    update(doc.id, { status: 'SENDING', detail: undefined });
    window.setTimeout(() => {
      const outcome = simulateOutcome(doc);
      update(doc.id, outcome);
    }, 1100);
  };

  const sendAll = () => docs.filter((d) => d.status === 'READY').forEach(sendOne);

  const sendable = docs.filter((d) => d.status === 'READY');
  const allConfirmed = docs.filter((d) => d.status !== 'DRAFT').every((d) => d.status === 'CONFIRMED') &&
    docs.some((d) => d.status === 'CONFIRMED');

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
      <SectionHeader
        title={labels.send.title}
        subtitle={labels.send.description}
        actions={
          <>
            <Link to={`/consultation/${appointmentId}/review`}>
              <BpButton variant="ghost" icon="arrow_back">{labels.review.title}</BpButton>
            </Link>
            <BpButton icon="send" disabled={sendable.length === 0} onClick={sendAll}>{labels.send.sendAll}</BpButton>
          </>
        }
      />

      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {docs.map((doc) => {
          const meta = STATUS_META[doc.status];
          return (
            <Card
              key={doc.id}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 14,
                borderLeft: `4px solid ${
                  doc.status === 'CONFIRMED'
                    ? 'var(--bp-ok-2)'
                    : doc.status === 'ERROR'
                      ? 'var(--bp-danger)'
                      : doc.status === 'QUEUED'
                        ? 'var(--bp-warn)'
                        : 'var(--bp-border)'
                }`,
              }}
            >
              <span style={{ width: 36, height: 36, borderRadius: 9, background: 'var(--bp-blue-tint)', color: 'var(--bp-blue-dark)', display: 'grid', placeItems: 'center' }}>
                <Icon name={doc.icon} size={20} />
              </span>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <strong style={{ fontSize: 14 }}>{doc.label}</strong>
                  {doc.required && <Pill tone="neutral">required</Pill>}
                </div>
                <div style={{ fontSize: 12, color: 'var(--bp-muted)', marginTop: 2 }}>
                  Receiving system: {doc.system}
                  {doc.detail && <span style={{ color: doc.status === 'ERROR' ? 'var(--bp-danger)' : 'var(--bp-warn)' }}> · {doc.detail}</span>}
                </div>
              </div>
              <Pill tone={meta.tone} icon={meta.icon}>{meta.label}</Pill>
              {doc.status === 'READY' && (
                <BpButton size="sm" icon="send" onClick={() => sendOne(doc)}>{labels.send.send}</BpButton>
              )}
              {(doc.status === 'ERROR' || doc.status === 'QUEUED') && (
                <BpButton size="sm" variant="secondary" icon="refresh" onClick={() => update(doc.id, { status: 'READY', detail: undefined })}>
                  {labels.send.retry}
                </BpButton>
              )}
            </Card>
          );
        })}
      </div>

      <div style={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'center', gap: 12 }}>
        {allConfirmed && <Pill tone="green" icon="verified">{labels.send.allConfirmed}</Pill>}
        <BpButton icon="arrow_forward" onClick={() => navigate(`/consultation/${appointmentId}/close`)}>
          {labels.send.next}
        </BpButton>
      </div>
    </div>
  );
}
