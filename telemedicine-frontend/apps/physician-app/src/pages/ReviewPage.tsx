import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { labels } from '../config/labels';
import { BpButton, BpTextArea, Card, Icon, Overline, Pill, SectionHeader } from '../ui/primitives';
import { MOCK_AI_DRAFT } from '../data/mockData';

/** Deck D-08: review the AI draft before sending; accept or reject (manual). */
export function ReviewPage() {
  const { appointmentId = '' } = useParams<{ appointmentId: string }>();
  const navigate = useNavigate();
  const draft = MOCK_AI_DRAFT[appointmentId] ?? MOCK_AI_DRAFT['AP-20931'];
  const [approved, setApproved] = useState(false);
  const [rejected, setRejected] = useState(false);

  const sections = [
    { key: 's', label: labels.review.soapS, value: draft.soap.s, icon: 'record_voice_over' },
    { key: 'o', label: labels.review.soapO, value: draft.soap.o, icon: 'vital_signs' },
    { key: 'a', label: labels.review.soapA, value: draft.soap.a, icon: 'clinical_notes' },
    { key: 'p', label: labels.review.soapP, value: draft.soap.p, icon: 'checklist' },
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
      <SectionHeader
        title={labels.review.title}
        subtitle={<span style={{ fontFamily: 'ui-monospace, Menlo, monospace' }}>{appointmentId}</span>}
        actions={
          <Link to={`/consultation/${appointmentId}`}>
            <BpButton variant="ghost" icon="arrow_back">{labels.workspace.title}</BpButton>
          </Link>
        }
      />

      <Card style={{ display: 'flex', alignItems: 'center', gap: 10, background: 'var(--bp-blue-tint-2)' }}>
        <Icon name="neurology" size={20} color="var(--bp-blue-dark)" />
        <span style={{ fontSize: 13, color: 'var(--bp-text-2)' }}>{labels.review.aiLabel}</span>
        <span style={{ marginLeft: 'auto' }}>
          {approved ? (
            <Pill tone="green" icon="verified">{labels.review.approvedBadge}</Pill>
          ) : rejected ? (
            <Pill tone="red" icon="edit">{labels.review.rejected}</Pill>
          ) : (
            <Pill tone="amber" icon="draft">{labels.review.draftBadge}</Pill>
          )}
        </span>
        <span style={{ fontSize: 11, color: 'var(--bp-muted)', fontFamily: 'ui-monospace, Menlo, monospace' }}>
          {draft.model} · {draft.version}
        </span>
      </Card>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 14 }}>
        {sections.map((s) => (
          <Card key={s.key} style={rejected ? { opacity: 0.5 } : undefined}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
              <Icon name={s.icon} size={18} color="var(--bp-blue)" />
              <Overline>{s.label}</Overline>
            </div>
            <BpTextArea defaultValue={s.value} rows={3} readOnly={approved} data-phi="true" />
          </Card>
        ))}
      </div>

      <Card>
        <Overline>{labels.review.instructions}</Overline>
        <BpTextArea defaultValue={draft.instructions} rows={2} style={{ marginTop: 8 }} readOnly={approved} data-phi="true" />
      </Card>

      <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
        {!approved && !rejected && (
          <>
            <BpButton variant="secondary" icon="edit" onClick={() => setRejected(true)}>{labels.review.reject}</BpButton>
            <BpButton icon="check" onClick={() => setApproved(true)}>{labels.review.accept}</BpButton>
          </>
        )}
        {(approved || rejected) && (
          <BpButton icon="arrow_forward" onClick={() => navigate(`/consultation/${appointmentId}/send`)}>
            {labels.review.next}
          </BpButton>
        )}
      </div>
    </div>
  );
}
