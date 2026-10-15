import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { labels } from '../config/labels';
import { BpButton, Card, Icon } from '../ui/primitives';

/** Deck D-14: consent withdrawn mid-call. Closes on a 30s countdown; no documents issued. */
export function WithdrawnPage() {
  const { appointmentId = '' } = useParams<{ appointmentId: string }>();
  const [count, setCount] = useState(30);
  const closed = count <= 0;

  useEffect(() => {
    if (closed) return;
    const id = window.setInterval(() => setCount((c) => Math.max(0, c - 1)), 1000);
    return () => window.clearInterval(id);
  }, [closed]);

  const closedTime = new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', second: '2-digit' });

  return (
    <div style={{ display: 'flex', justifyContent: 'center', paddingTop: 24 }}>
      <Card style={{ maxWidth: 520, textAlign: 'center', padding: 40, borderTop: '4px solid var(--bp-danger)' }}>
        <span style={{ width: 64, height: 64, borderRadius: '50%', background: 'var(--bp-danger-bg)', display: 'grid', placeItems: 'center', margin: '0 auto 16px' }}>
          <Icon name={closed ? 'gavel' : 'block'} size={32} color="var(--bp-danger)" fill />
        </span>
        <h2 style={{ margin: '0 0 8px', fontSize: 20 }}>{labels.withdrawn.title}</h2>
        <p style={{ color: 'var(--bp-muted)', lineHeight: 1.6, margin: '0 0 20px' }}>{labels.withdrawn.message}</p>

        {closed ? (
          <>
            <div style={{ padding: 14, borderRadius: 10, background: 'var(--bp-surface-2)' }}>
              <strong>{labels.withdrawn.closedAt} {closedTime}</strong>
              <div style={{ fontSize: 13, color: 'var(--bp-muted)', marginTop: 4 }}>{labels.withdrawn.noDocuments}</div>
            </div>
            <div style={{ marginTop: 20 }}>
              <Link to="/worklist?view=after">
                <BpButton icon="list_alt">{labels.close.backToWorklist}</BpButton>
              </Link>
            </div>
          </>
        ) : (
          <>
            <div style={{ fontSize: 13, color: 'var(--bp-muted)' }}>{labels.withdrawn.countdown}</div>
            <div style={{ fontSize: 48, fontWeight: 800, fontFamily: 'ui-monospace, Menlo, monospace', color: 'var(--bp-danger)', margin: '4px 0' }}>
              {count}s
            </div>
            <div style={{ fontSize: 12, color: 'var(--bp-muted-2)' }}>
              Consultation: <span style={{ fontFamily: 'ui-monospace, Menlo, monospace' }}>{appointmentId}</span>
            </div>
          </>
        )}
      </Card>
    </div>
  );
}
