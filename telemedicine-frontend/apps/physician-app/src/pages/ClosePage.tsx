import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { labels } from '../config/labels';
import { BpButton, BpTextArea, Card, Field, Icon, Pill, SectionHeader } from '../ui/primitives';
import { sendDocsFor } from '../data/mockData';

/** Deck D-10: close consultation. Closing checks all documents confirmed, or records an exception. */
export function ClosePage() {
  const { appointmentId = '' } = useParams<{ appointmentId: string }>();
  const navigate = useNavigate();
  const docs = sendDocsFor(appointmentId).filter((d) => d.status !== 'DRAFT');
  // For the demo, treat the happy path (Priya Sharma) as all-confirmed.
  const allConfirmed = appointmentId === 'AP-20931';
  const [showException, setShowException] = useState(false);
  const [reason, setReason] = useState('');
  const [closed, setClosed] = useState(false);

  if (closed) {
    return (
      <Card style={{ textAlign: 'center', padding: 48, borderTop: '4px solid var(--bp-ok-2)' }}>
        <Icon name="check_circle" size={44} color="var(--bp-ok)" fill />
        <h2 style={{ margin: '12px 0 4px' }}>{labels.close.closed}</h2>
        <p style={{ color: 'var(--bp-muted)' }}>
          {allConfirmed ? labels.send.allConfirmed : `Exception recorded: ${reason || '—'}`}
        </p>
        <div style={{ marginTop: 16 }}>
          <Link to="/worklist?view=after">
            <BpButton icon="list_alt">{labels.close.backToWorklist}</BpButton>
          </Link>
        </div>
      </Card>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
      <SectionHeader
        title={labels.close.title}
        subtitle={labels.close.description}
        actions={
          <Link to={`/consultation/${appointmentId}/send`}>
            <BpButton variant="ghost" icon="arrow_back">{labels.send.title}</BpButton>
          </Link>
        }
      />

      <Card style={{ borderLeft: `4px solid ${allConfirmed ? 'var(--bp-ok-2)' : 'var(--bp-warn)'}` }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 12 }}>
          <Icon name={allConfirmed ? 'verified' : 'warning'} size={22} color={allConfirmed ? 'var(--bp-ok)' : 'var(--bp-warn)'} />
          <strong>{allConfirmed ? labels.close.allConfirmed : labels.close.pending}</strong>
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          {docs.map((d) => (
            <div key={d.id} style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '8px 10px', background: 'var(--bp-surface-2)', borderRadius: 8 }}>
              <Icon name={d.icon} size={18} color="var(--bp-blue)" />
              <span style={{ flex: 1 }}>{d.label}</span>
              <Pill tone={allConfirmed ? 'green' : 'amber'} icon={allConfirmed ? 'check_circle' : 'hourglass_top'}>
                {allConfirmed ? labels.send.statusCONFIRMED : 'Pending'}
              </Pill>
            </div>
          ))}
        </div>
      </Card>

      {showException && (
        <Card>
          <Field label={labels.close.exceptionReason}>
            <BpTextArea value={reason} onChange={(e) => setReason(e.target.value)} rows={2} />
          </Field>
        </Card>
      )}

      <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
        {allConfirmed ? (
          <BpButton icon="lock" onClick={() => setClosed(true)}>{labels.close.closeAction}</BpButton>
        ) : !showException ? (
          <BpButton variant="secondary" icon="report" onClick={() => setShowException(true)}>{labels.close.exception}</BpButton>
        ) : (
          <BpButton variant="danger" icon="lock" disabled={!reason.trim()} onClick={() => setClosed(true)}>
            {labels.close.exception}
          </BpButton>
        )}
      </div>
    </div>
  );
}
