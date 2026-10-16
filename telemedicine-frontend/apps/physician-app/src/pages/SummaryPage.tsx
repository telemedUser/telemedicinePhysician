import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { usePhysicianApi } from '../api/physicianApi';
import type { WorklistEntry } from '../api/types';
import {
  CheckItem,
  Icon,
  InfoCard,
  KeyValue,
  PageBack,
  PageBody,
  PageHeader,
  Pill,
  Section,
} from '../ui/primitives';

function initials(name?: string) {
  if (!name) return '··';
  return name
    .replace(/\(.*?\)/g, '')
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((p) => p[0]?.toUpperCase() ?? '')
    .join('');
}

function clockOf(iso?: string) {
  if (!iso) return '—';
  const d = new Date(iso);
  return Number.isNaN(d.getTime())
    ? '—'
    : d.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
}

const inr = (n: number) =>
  new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(n);

/**
 * Completed-consultation summary (reached from the worklist "View summary" action). Read-only
 * recap: who/when, reason for visit, medical history, the consultation completion checklist and
 * the payment breakdown. Uses the shared detail-page layout primitives so it matches every other
 * page's colour scheme and structure.
 */
export function SummaryPage() {
  const { appointmentId = '' } = useParams<{ appointmentId: string }>();
  const navigate = useNavigate();
  const api = usePhysicianApi();
  const [entry, setEntry] = useState<WorklistEntry | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    void (async () => {
      const rows = await api.getWorklist();
      if (!active) return;
      setEntry(rows.find((e) => e.appointmentId === appointmentId) ?? null);
      setLoading(false);
    })();
    return () => {
      active = false;
    };
  }, [api, appointmentId]);

  const name = entry?.patientRef ?? 'Patient';
  const time = clockOf(entry?.enteredAt);
  const mode = entry?.mode === 'audio' ? 'Audio' : 'Video';

  // Representative completed-consultation detail (no clinical body is stored by the platform).
  const fee = 1200;
  const voucher = 300;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
      <PageBack label={labelWorklist} onClick={() => navigate('/worklist')} />

      <PageHeader
        initials={initials(name)}
        name={name}
        statusPill={
          <Pill tone="green" icon="task_alt">
            Completed
          </Pill>
        }
        meta={
          <>
            <span>
              <Icon name="schedule" size={15} />
              {time}
            </span>
            <span>
              <Icon name={entry?.mode === 'audio' ? 'call' : 'videocam'} size={15} />
              {mode}
            </span>
          </>
        }
        banner={{
          icon: 'check_circle',
          title: 'Consultation completed',
          sub: (
            <>
              <Icon name="info" size={14} />
              Documents 3 of 3 delivered
            </>
          ),
        }}
      />

      {loading ? (
        <div style={{ padding: 24, color: 'var(--bp-muted)' }}>Loading…</div>
      ) : (
        <PageBody
          main={
            <>
              <Section label="Reason for visit">
                <div className="pg-section-lead">
                  <Icon name="task_alt" size={20} color="var(--bp-ok)" />
                  <span>
                    <strong>Consultation completed at {time}.</strong> Intake, notes and documents
                    are on the encounter.
                  </span>
                </div>
                <span className="pg-link" onClick={() => navigate(`/consultation/${appointmentId}`)}>
                  Open encounter in Connected Care
                </span>
              </Section>

              <Section label="Medical history">
                <div className="pg-section-lead">
                  <span>
                    <strong>Confirmed at intake.</strong> The Connected Care record is up to date.
                  </span>
                </div>
              </Section>
            </>
          }
          rail={
            <>
              <InfoCard title="Consultation">
                <CheckItem title="Consultation" sub={`Completed ${time}`} />
                <CheckItem title="Documents" sub="3 of 3 delivered" />
              </InfoCard>

              <InfoCard
                title="Payment"
                accessory={
                  <Pill tone="green" icon="check_circle">
                    Paid
                  </Pill>
                }
              >
                <KeyValue label="Consultation fee" value={inr(fee)} />
                <KeyValue label="Insurance voucher" value={`– ${inr(voucher)}`} />
                <KeyValue label="Paid after voucher" value={inr(fee - voucher)} total />
                <div className="pg-kv-note">Receipt No. 982114 · Online · {time}</div>
              </InfoCard>
            </>
          }
        />
      )}
    </div>
  );
}

const labelWorklist = 'Worklist';
