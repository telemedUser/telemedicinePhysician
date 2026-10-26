import { useEffect, useMemo, useState, type ReactNode } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { usePhysicianApi } from '../api/physicianApi';
import type { CarePlan, PatientDetail } from '../api/types';
import { Icon } from '../ui/primitives';
import { CarePlanStepper } from './careplan/CarePlanStepper';

/**
 * D-09 Send — a read-only summary of everything captured on D-08 (the care plan), shown as an
 * accordion of six routing rows: Clinical note, Diagnosis, Prescription, Follow-up, Medical leave
 * and the patient Summary. Nothing here is authored: every value is read back from the saved care
 * plan (GET care-plan). "Edit in Review" deep-links back to D-08 at the relevant section.
 */

type SendStatus = 'READY' | 'SENDING' | 'SENT';
const STATUS_LABEL: Record<SendStatus, string> = { READY: 'Ready to send', SENDING: 'Sending…', SENT: 'Sent' };

/** Each care-plan area routes to a downstream system; these labels are the routing destinations. */
const ROUTES = {
  note: 'Clinical record',
  diagnosis: 'Clinical record · Pharmacy network',
  prescription: 'Pharmacy network',
  followUp: 'Booking system',
  medicalLeave: 'Leave registry',
  summary: 'Patient app',
} as const;

const DATE_FMT: Intl.DateTimeFormatOptions = { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' };
function fmtDate(value?: string | null): string {
  if (!value) return '';
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? '' : d.toLocaleDateString('en-GB', DATE_FMT);
}
function inclusiveDays(start?: string, end?: string): number {
  if (!start || !end) return 0;
  const s = new Date(start);
  const e = new Date(end);
  if (Number.isNaN(s.getTime()) || Number.isNaN(e.getTime())) return 0;
  return Math.max(1, Math.round((e.getTime() - s.getTime()) / 86400000) + 1);
}

export function SendPage() {
  const { appointmentId = '' } = useParams<{ appointmentId: string }>();
  const navigate = useNavigate();
  const api = usePhysicianApi();

  const [plan, setPlan] = useState<CarePlan | null>(null);
  const [patient, setPatient] = useState<PatientDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [open, setOpen] = useState<string | null>(null);
  const [statuses, setStatuses] = useState<Record<string, SendStatus>>({});
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    let active = true;
    void Promise.all([api.getCarePlan(appointmentId), api.getPatientDetail(appointmentId)]).then(([p, d]) => {
      if (!active) return;
      setPlan(p);
      setPatient(d);
      setLoading(false);
    });
    return () => {
      active = false;
    };
  }, [api, appointmentId]);

  const toggle = (id: string) => setOpen((cur) => (cur === id ? null : id));
  const editIn = (section: string) => navigate(`/consultation/${appointmentId}/review#section=${section}`);

  // First name for the patient-facing summary row title ("Summary and instructions for <name>").
  const firstName = useMemo(() => (patient?.patientDisplay ?? '').trim().split(/\s+/)[0] ?? '', [patient]);

  const rows = useMemo(() => buildRows(plan, firstName), [plan, firstName]);

  const submitAll = async () => {
    setSubmitting(true);
    // Mark each present row as sending, then sent (the care plan is already persisted on D-08).
    setStatuses(Object.fromEntries(rows.map((r) => [r.id, 'SENDING' as SendStatus])));
    await new Promise((res) => window.setTimeout(res, 900));
    setStatuses(Object.fromEntries(rows.map((r) => [r.id, 'SENT' as SendStatus])));
    setSubmitting(false);
    window.setTimeout(() => navigate(`/consultation/${appointmentId}/close`), 500);
  };

  const headerMeta = [patient?.modality ?? undefined, fmtTimeRange(patient?.scheduledAt)]
    .filter(Boolean)
    .join(' · ');

  if (loading) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
        <CarePlanStepper current={1} />
        <div style={{ padding: 24, color: 'var(--bp-muted)' }}>Loading…</div>
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 16, flexWrap: 'wrap' }}>
        <span style={{ fontSize: 14 }}>
          <strong style={{ color: 'var(--bp-navy)' }} data-phi="true">{patient?.patientDisplay ?? appointmentId}</strong>
          {headerMeta && <span style={{ color: 'var(--bp-muted)' }}> · {headerMeta}</span>}
        </span>
        <span style={{ flex: 1 }} />
        <CarePlanStepper current={1} />
      </div>

      <h1 className="cp-title">Send</h1>

      <div className="sd-card">
        {rows.map((row) => {
          const status = statuses[row.id] ?? 'READY';
          const isOpen = open === row.id;
          return (
            <div className="sd-row" key={row.id}>
              <button type="button" className="sd-head" aria-expanded={isOpen} onClick={() => toggle(row.id)}>
                <span className="sd-icon"><Icon name={row.icon} size={20} /></span>
                <span className="sd-headmain">
                  <span className="sd-title">{row.title}</span>
                  <span className="sd-sub"><Icon name="arrow_forward" size={15} />{row.dest}</span>
                </span>
                <span className={status === 'SENT' ? 'sd-status is-sent' : status === 'SENDING' ? 'sd-status is-sending' : 'sd-status'}>
                  <Icon name={status === 'SENT' ? 'check_circle' : 'schedule_send'} size={15} />
                  {STATUS_LABEL[status]}
                </span>
                <span className={isOpen ? 'sd-chev is-open' : 'sd-chev'}><Icon name="expand_more" size={22} /></span>
              </button>

              {isOpen && (
                <div className="sd-body">
                  {row.body}
                  <button type="button" className="sd-edit" onClick={() => editIn(row.section)}>
                    <Icon name="edit" size={16} />Edit in Review
                  </button>
                </div>
              )}
            </div>
          );
        })}
      </div>

      <div className="sd-actions">
        <button type="button" className="sd-submit" disabled={submitting || rows.length === 0} onClick={() => void submitAll()}>
          Submit all <Icon name="send" size={18} color="#fff" />
        </button>
        <button type="button" className="sd-back" onClick={() => navigate(`/consultation/${appointmentId}/review`)}>
          Back to review
        </button>
      </div>
    </div>
  );
}

/** Time range like "16:46–17:00 · 14 min" from a scheduled ISO time (duration unknown → omitted). */
function fmtTimeRange(scheduledAt?: string | null): string {
  if (!scheduledAt) return '';
  const d = new Date(scheduledAt);
  if (Number.isNaN(d.getTime())) return '';
  const t = d.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
  return t;
}

interface SendRow {
  id: string;
  section: string;
  icon: string;
  title: string;
  dest: string;
  body: ReactNode;
}

/** Derive the six accordion rows purely from the saved care plan. Rows present only when they hold data. */
function buildRows(plan: CarePlan | null, firstName: string): SendRow[] {
  if (!plan) return [];
  const rows: SendRow[] = [];

  // 1) Clinical note — always shown (the core document).
  const note = plan.note ?? {};
  rows.push({
    id: 'note',
    section: 'note',
    icon: 'clinical_notes',
    title: 'Clinical note',
    dest: ROUTES.note,
    body: (
      <>
        {([
          ['History', note.history],
          ['Findings', note.findings],
          ['Assessment', note.assessment],
          ['Plan', note.plan],
        ] as const).map(([label, val]) => (
          <div key={label}>
            <div className="sd-field-label">{label}</div>
            <div className="sd-field-value">{val?.trim() ? val : 'Not documented'}</div>
          </div>
        ))}
      </>
    ),
  });

  // 2) Diagnosis.
  const primary = plan.diagnoses.find((d) => d.primary) ?? plan.diagnoses[0];
  rows.push({
    id: 'diagnosis',
    section: 'diagnosis',
    icon: 'description',
    title: primary ? `Diagnosis · ${primary.code}` : 'Diagnosis',
    dest: ROUTES.diagnosis,
    body:
      plan.diagnoses.length > 0 ? (
        <>
          {plan.diagnoses.map((d) => (
            <div className="sd-line" key={d.code}>
              {d.code} {d.title}
              {d.primary ? ' · primary' : ''}
            </div>
          ))}
        </>
      ) : (
        <div className="sd-field-value">No diagnosis recorded</div>
      ),
  });

  // 3) Prescription (medicines + ordered diagnostic tests).
  const medCount = plan.prescriptions.length;
  rows.push({
    id: 'prescription',
    section: 'prescription',
    icon: 'prescriptions',
    title: medCount > 0 ? `Prescription · ${medCount} medicine${medCount === 1 ? '' : 's'}` : 'Prescription',
    dest: ROUTES.prescription,
    body:
      medCount > 0 || plan.diagnosticTests.length > 0 ? (
        <>
          {medCount > 0 && (
            <>
              <div className="sd-field-label">Medicines</div>
              {plan.prescriptions.map((m, i) => {
                const head = `${m.name}${m.strength ? ` ${m.strength}` : ''}`;
                const detail = [m.dosage, m.frequency, m.durationDays ? `${m.durationDays} days` : null, m.quantity ? `Qty ${m.quantity}` : null]
                  .filter(Boolean)
                  .join(' · ');
                return <div className="sd-line" key={i}>{head}{detail ? ` · ${detail}` : ''}</div>;
              })}
            </>
          )}
          {plan.diagnosticTests.length > 0 && (
            <>
              <div className="sd-field-label">Diagnostic tests</div>
              {plan.diagnosticTests.map((t, i) => (
                <div className="sd-line" key={i}>{t.name}{t.category ? ` · ${t.category}` : ''}</div>
              ))}
            </>
          )}
        </>
      ) : (
        <div className="sd-field-value">No medicines or tests ordered</div>
      ),
  });

  // 4) Follow-up (only meaningful when enabled).
  const fu = plan.followUp;
  rows.push({
    id: 'followUp',
    section: 'follow-up',
    icon: 'event_repeat',
    title: fu.enabled ? `Follow-up · ${fu.mode?.trim() || 'Video check-in'}` : 'Follow-up · none',
    dest: ROUTES.followUp,
    body: fu.enabled ? (
      <div className="sd-line">
        {fu.mode?.trim() || 'Video check-in'}
        {fu.inDays ? ` · in ${fu.inDays} day${fu.inDays === 1 ? '' : 's'}` : ''}
        {fu.date ? ` · ${fmtDate(fu.date)}` : ''}
        {fu.instructions?.trim() ? ` · ${fu.instructions}` : ''}
      </div>
    ) : (
      <div className="sd-field-value">No follow-up scheduled</div>
    ),
  });

  // 5) Medical leave (only meaningful when enabled).
  const ml = plan.medicalLeave;
  const leaveDays = inclusiveDays(ml.startDate, ml.endDate);
  rows.push({
    id: 'medicalLeave',
    section: 'medical-leave',
    icon: 'event_busy',
    title: ml.enabled ? `Medical leave · ${leaveDays} day${leaveDays === 1 ? '' : 's'}` : 'Medical leave · none',
    dest: ROUTES.medicalLeave,
    body: ml.enabled ? (
      <div className="sd-line">
        {ml.reason?.trim() || 'Medical leave'}
        {leaveDays ? ` · ${leaveDays} day${leaveDays === 1 ? '' : 's'}` : ''}
        {ml.startDate ? ` · from ${fmtDate(ml.startDate)}` : ''}
      </div>
    ) : (
      <div className="sd-field-value">No medical leave issued</div>
    ),
  });

  // 6) Patient summary + instructions.
  const sum = plan.summary ?? {};
  rows.push({
    id: 'summary',
    section: 'summary',
    icon: 'summarize',
    title: firstName ? `Summary and instructions for ${firstName}` : 'Summary and instructions',
    dest: ROUTES.summary,
    body: (
      <>
        <div className="sd-field-label">Summary</div>
        <div className="sd-field-value">{sum.text?.trim() ? sum.text : 'Not written'}</div>
        <div className="sd-field-label">Instructions</div>
        <div className="sd-field-value">{sum.instructions?.trim() ? sum.instructions : 'Not written'}</div>
      </>
    ),
  });

  return rows;
}
