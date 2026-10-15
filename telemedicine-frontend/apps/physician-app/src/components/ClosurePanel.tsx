import { useState } from 'react';
import { usePhysicianApi } from '../api/physicianApi';
import type { CompletionDocument } from '../api/types';
import { labels } from '../config/labels';
import { BpButton, BpTextArea, Card, Field, Icon, Pill } from '../ui/primitives';

const DOCS: { key: CompletionDocument; label: string; required?: boolean }[] = [
  { key: 'NOTE', label: labels.closure.note, required: true },
  { key: 'DIAGNOSIS', label: labels.closure.diagnosis, required: true },
  { key: 'PRESCRIPTION', label: labels.closure.prescription },
  { key: 'CERTIFICATE', label: labels.closure.certificate },
  { key: 'REFERRAL', label: labels.closure.referral },
  { key: 'FOLLOW_UP', label: labels.closure.followUp },
];

export function ClosurePanel({ appointmentId, onClosed }: { appointmentId: string; onClosed: () => void }) {
  const api = usePhysicianApi();
  const [completed, setCompleted] = useState<Set<CompletionDocument>>(new Set(['NOTE', 'DIAGNOSIS']));
  const [exceptionReason, setExceptionReason] = useState('');
  const [missing, setMissing] = useState<string[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [showException, setShowException] = useState(false);

  const toggle = (doc: CompletionDocument) =>
    setCompleted((prev) => {
      const next = new Set(prev);
      next.has(doc) ? next.delete(doc) : next.add(doc);
      return next;
    });

  const submit = async () => {
    setSubmitting(true);
    setMissing([]);
    try {
      const result = await api.closeConsultation(appointmentId, {
        completedDocuments: Array.from(completed),
        exceptionReason: exceptionReason.trim() || undefined,
      });
      if (result.closed) onClosed();
      else {
        setMissing(result.missingDocuments);
        setShowException(true);
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Card>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 }}>
        <Icon name="task_alt" size={20} color="var(--bp-blue)" />
        <strong>{labels.closure.title}</strong>
      </div>
      <p style={{ margin: '0 0 12px', fontSize: 13, color: 'var(--bp-muted)', lineHeight: 1.5 }}>
        {labels.closure.description}
      </p>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
        {DOCS.map((doc) => {
          const on = completed.has(doc.key);
          return (
            <button
              key={doc.key}
              type="button"
              onClick={() => toggle(doc.key)}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 10,
                padding: '9px 12px',
                borderRadius: 8,
                border: `1px solid ${on ? 'var(--bp-ok-2)' : 'var(--bp-border)'}`,
                background: on ? 'var(--bp-ok-bg)' : 'var(--bp-surface)',
                cursor: 'pointer',
                textAlign: 'left',
                font: 'inherit',
              }}
            >
              <Icon
                name={on ? 'check_circle' : 'radio_button_unchecked'}
                size={20}
                color={on ? 'var(--bp-ok)' : 'var(--bp-muted-2)'}
                fill={on}
              />
              <span style={{ flex: 1, fontWeight: 500 }}>{doc.label}</span>
              {doc.required && <Pill tone={on ? 'green' : 'neutral'}>required</Pill>}
            </button>
          );
        })}
      </div>

      {(showException || missing.length > 0) && (
        <div style={{ marginTop: 12 }}>
          {missing.length > 0 && (
            <div
              style={{
                display: 'flex',
                gap: 8,
                padding: 10,
                borderRadius: 8,
                background: 'var(--bp-warn-bg)',
                color: 'var(--bp-warn)',
                fontSize: 13,
                marginBottom: 10,
              }}
            >
              <Icon name="info" size={18} />
              <span>
                {labels.closure.missing} {missing.join(', ')}
              </span>
            </div>
          )}
          <Field label={labels.closure.exceptionLabel}>
            <BpTextArea value={exceptionReason} onChange={(e) => setExceptionReason(e.target.value)} rows={2} />
          </Field>
        </div>
      )}

      <div style={{ marginTop: 14, display: 'flex', justifyContent: 'flex-end' }}>
        <BpButton variant="danger" icon="lock" loading={submitting} onClick={() => void submit()}>
          {labels.closure.submit}
        </BpButton>
      </div>
    </Card>
  );
}
