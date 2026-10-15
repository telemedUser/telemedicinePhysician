import { useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  Avatar,
  BpButton,
  BpInput,
  Card,
  Icon,
  Overline,
  Pill,
  SectionHeader,
  type Tone,
} from '../ui/primitives';
import { MOCK_PREVIOUS_PATIENTS, type PreviousPatient } from '../data/mockData';

const OUTCOME: Record<PreviousPatient['outcome'], { tone: Tone; label: string }> = {
  COMPLETED: { tone: 'green', label: 'Completed' },
  CONVERTED_IN_PERSON: { tone: 'amber', label: 'Converted to in-person' },
  NO_SHOW: { tone: 'red', label: 'No-show' },
};

function initials(name: string) {
  return name
    .replace(/\(.*?\)/g, '')
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((p) => p[0]?.toUpperCase() ?? '')
    .join('');
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleString('en-IN', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

/**
 * Previous patients — closed/past consultations with their consultation note and prescription.
 * (Demo data; in the full backend this reads the consultation-closure records + issued documents.)
 */
export function PreviousPatientsPage() {
  const [searchParams] = useSearchParams();
  const initialQuery = searchParams.get('q') ?? '';
  const [query, setQuery] = useState(initialQuery);
  const [openId, setOpenId] = useState<string | null>(null);

  const visible = useMemo(() => {
    const q = query.trim().toLowerCase();
    const rows = [...MOCK_PREVIOUS_PATIENTS].sort(
      (a, b) => new Date(b.consultedOn).getTime() - new Date(a.consultedOn).getTime(),
    );
    if (!q) return rows;
    return rows.filter(
      (p) =>
        p.patientName.toLowerCase().includes(q) ||
        p.abhaId.toLowerCase().includes(q) ||
        p.diagnosis.toLowerCase().includes(q) ||
        p.appointmentId.toLowerCase().includes(q),
    );
  }, [query]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
      <SectionHeader
        title="Previous patients"
        subtitle="Closed consultations with their notes and prescriptions."
      />

      <Card style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
        <Icon name="search" size={18} color="var(--bp-muted)" />
        <BpInput
          placeholder="Search by patient, ABHA ID, diagnosis or appointment"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
      </Card>

      {visible.length === 0 ? (
        <Card style={{ textAlign: 'center', padding: 40 }}>
          <Icon name="history" size={32} color="var(--bp-muted-2)" />
          <p style={{ color: 'var(--bp-muted)' }}>No previous consultations match your search.</p>
        </Card>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {visible.map((p) => {
            const open = openId === p.appointmentId;
            const outcome = OUTCOME[p.outcome];
            return (
              <Card key={p.appointmentId} style={{ display: 'flex', flexDirection: 'column', gap: 0 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
                  <Avatar initials={initials(p.patientName)} size={44} tone="navy" />
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
                      <strong style={{ fontSize: 15 }} data-phi="true">
                        {p.patientName}
                      </strong>
                      <span style={{ color: 'var(--bp-muted-2)', fontSize: 12 }}>
                        {p.age} yrs · {p.sex}
                      </span>
                      <Pill tone={outcome.tone} dot>
                        {outcome.label}
                      </Pill>
                    </div>
                    <div style={{ marginTop: 4, fontSize: 13, color: 'var(--bp-muted)' }}>
                      {p.diagnosis} ·{' '}
                      <span style={{ fontFamily: 'ui-monospace, Menlo, monospace' }}>{p.diagnosisCode}</span>
                    </div>
                  </div>
                  <div style={{ textAlign: 'right', flex: 'none' }}>
                    <div style={{ fontSize: 12, color: 'var(--bp-muted)' }}>{formatDate(p.consultedOn)}</div>
                    <div style={{ fontSize: 12, color: 'var(--bp-muted-2)' }}>
                      <Icon name={p.mode === 'audio' ? 'call' : 'videocam'} size={13} /> {p.mode} ·{' '}
                      {p.durationMinutes} min
                    </div>
                  </div>
                  <BpButton
                    variant="secondary"
                    icon={open ? 'expand_less' : 'expand_more'}
                    onClick={() => setOpenId(open ? null : p.appointmentId)}
                  >
                    {open ? 'Hide' : 'View'}
                  </BpButton>
                </div>

                {open && (
                  <div
                    style={{
                      marginTop: 14,
                      paddingTop: 14,
                      borderTop: '1px solid var(--bp-border-2)',
                      display: 'grid',
                      gridTemplateColumns: '1fr 1fr',
                      gap: 18,
                    }}
                  >
                    <div>
                      <Overline>Consultation note</Overline>
                      <p
                        style={{ margin: '8px 0 0', fontSize: 14, lineHeight: 1.6, color: 'var(--bp-text-2)' }}
                        data-phi="true"
                      >
                        {p.consultationNote}
                      </p>
                      {p.followUp && (
                        <div style={{ marginTop: 10 }}>
                          <Pill tone="blue" icon="event_repeat">
                            Follow-up: {p.followUp}
                          </Pill>
                        </div>
                      )}
                      <div style={{ marginTop: 10, fontSize: 12, color: 'var(--bp-muted-2)', fontFamily: 'ui-monospace, Menlo, monospace' }}>
                        {p.appointmentId} · {p.abhaId}
                      </div>
                    </div>

                    <div>
                      <Overline>Prescription</Overline>
                      <div style={{ marginTop: 8, display: 'flex', flexDirection: 'column', gap: 8 }}>
                        {p.prescription.length === 0 ? (
                          <span style={{ color: 'var(--bp-muted)', fontSize: 14 }}>No medication prescribed.</span>
                        ) : (
                          p.prescription.map((rx, i) => (
                            <div
                              key={i}
                              style={{
                                display: 'flex',
                                gap: 10,
                                alignItems: 'flex-start',
                                padding: '8px 10px',
                                background: 'var(--bp-surface-2)',
                                borderRadius: 8,
                              }}
                            >
                              <Icon name="prescriptions" size={18} color="var(--bp-blue)" />
                              <div>
                                <strong style={{ fontSize: 14 }}>{rx.drug}</strong>
                                <div style={{ fontSize: 12, color: 'var(--bp-muted)' }}>
                                  {rx.dosage} · {rx.frequency} · {rx.duration}
                                </div>
                              </div>
                            </div>
                          ))
                        )}
                      </div>
                    </div>
                  </div>
                )}
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
}
