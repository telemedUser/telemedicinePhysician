import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { usePhysicianApi } from '../api/physicianApi';
import type { PatientDetail } from '../api/types';
import { deriveWorklistStatus } from '../api/worklistStatus';
import { labels } from '../config/labels';
import {
  BpButton,
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

function money(minor: number, currency: string) {
  const major = (minor ?? 0) / 100;
  try {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: currency || 'INR',
      maximumFractionDigits: 0,
    }).format(major);
  } catch {
    return `${currency} ${major.toFixed(0)}`;
  }
}

function clock(iso?: string | null) {
  if (!iso) return '—';
  const d = new Date(iso);
  return Number.isNaN(d.getTime())
    ? '—'
    : d.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
}

const SEX_LABEL: Record<string, string> = { F: 'Female', M: 'Male', X: 'Other' };

const VISIT_LABEL: Record<string, string> = {
  FIRST_VIRTUAL: 'First virtual visit',
  FOLLOW_UP: 'Follow-up',
};

const DURATION_LABEL: Record<string, string> = {
  HOURS_FEW: 'A few hours',
  DAYS_1: '1 day',
  DAYS_2: '2 days',
  DAYS_3: '3 days',
  WEEK_1: '1 week',
  WEEKS_2: '2 weeks',
};

/**
 * Physician "review & join" detail (prototype D-03). Every value is read from the physician-service
 * detail endpoint, which is projected from the patient service's ConsultationContextPublished Kafka
 * event. Nothing clinical is hard-coded here — fields the event did not carry render as empty /
 * "none recorded". The Join button is gated on readiness: it only appears once the patient has
 * arrived in the waiting room; a not-arrived patient cannot be joined.
 */
export function PatientReviewPage() {
  const { appointmentId = '' } = useParams<{ appointmentId: string }>();
  const navigate = useNavigate();
  const api = usePhysicianApi();

  const [detail, setDetail] = useState<PatientDetail | null>(null);
  const [state, setState] = useState<'loading' | 'ready' | 'empty'>('loading');
  const [joining, setJoining] = useState(false);

  const join = async () => {
    setJoining(true);
    try {
      // Admit the patient (worklist → OPEN_DOCTOR) then open the consultation workspace.
      await api.admitPatient(appointmentId);
      navigate(`/consultation/${appointmentId}`);
    } finally {
      setJoining(false);
    }
  };

  useEffect(() => {
    let active = true;
    void (async () => {
      const d = await api.getPatientDetail(appointmentId);
      if (!active) return;
      setDetail(d);
      setState(d ? 'ready' : 'empty');
    })();
    return () => {
      active = false;
    };
  }, [api, appointmentId]);

  if (state === 'loading') {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
        <PageBack label={labels.nav.worklist} onClick={() => navigate('/worklist')} />
        <div style={{ padding: 24, color: 'var(--bp-muted)' }}>{labels.common.loading}</div>
      </div>
    );
  }

  if (state === 'empty' || !detail) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
        <PageBack label={labels.nav.worklist} onClick={() => navigate('/worklist')} />
        <div className="pg-infocard" style={{ textAlign: 'center', padding: 40 }}>
          <Icon name="cloud_off" size={32} color="var(--bp-muted-2)" />
          <p style={{ color: 'var(--bp-muted)' }}>
            No review details are available for this appointment yet.
          </p>
        </div>
      </div>
    );
  }

  const status = deriveWorklistStatus(detail);
  const ctx = detail.clinicalContext ?? {};
  const mode = (detail.modality ?? 'VIDEO').toLowerCase();
  const name = detail.patientDisplay ?? appointmentId;

  // Join is only possible once the patient has arrived in the waiting room. Ready = enabled;
  // in waiting room (checks in progress) = shown but disabled; not arrived = no button.
  const canJoin = status.joinable;
  const showJoin = status.arrived && status.key !== 'COMPLETED';

  const metaBits: string[] = [];
  if (detail.ageYears > 0) metaBits.push(`${detail.ageYears}`);
  if (detail.sexCode) metaBits.push(SEX_LABEL[detail.sexCode] ?? detail.sexCode);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
      <PageBack label={labels.nav.worklist} onClick={() => navigate('/worklist')} />

      <PageHeader
        initials={initials(name)}
        name={name}
        statusPill={
          detail.redFlag ? (
            <span className="wl-redflag">
              <Icon name="emergency" size={16} fill />
              {labels.flags.RED_FLAG}
            </span>
          ) : (
            <Pill tone={status.chip === 'neutral' ? 'blue' : status.chip} icon={status.icon}>
              {status.label}
            </Pill>
          )
        }
        meta={
          <>
            {metaBits.length > 0 && (
              <span>
                <Icon name="person" size={15} />
                {metaBits.join(' · ')}
              </span>
            )}
            {detail.insuranceScheme && (
              <span>
                <Icon name="shield" size={15} />
                {detail.insuranceScheme}
              </span>
            )}
            {detail.nationalIdMasked && (
              <span style={{ fontFamily: 'ui-monospace, Menlo, monospace' }}>
                {detail.nationalIdMasked}
              </span>
            )}
            {detail.city && (
              <span>
                <Icon name="location_on" size={15} />
                {detail.city}
              </span>
            )}
            <span>
              <Icon name="schedule" size={15} />
              {clock(detail.scheduledAt)}
            </span>
            <span>
              <Icon name={mode === 'audio' ? 'call' : 'videocam'} size={15} />
              {mode === 'audio' ? 'Audio' : 'Video'}
            </span>
            {detail.visitType && (
              <span>
                <Icon name="event_available" size={15} />
                {VISIT_LABEL[detail.visitType] ?? detail.visitType}
              </span>
            )}
          </>
        }
      />

      {/* Join action — gated on arrival/readiness. */}
      {showJoin && (
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
          <BpButton
            icon="videocam"
            disabled={!canJoin || joining}
            onClick={() => void join()}
          >
            Join consultation
          </BpButton>
          {!canJoin && (
            <span style={{ fontSize: 13, color: 'var(--bp-muted)' }}>
              {status.detail || 'Waiting for the patient to be ready.'}
            </span>
          )}
        </div>
      )}

      <PageBody
        main={
          <>
            <Section label="Reason for visit">
              <div className="pg-section-lead">
                <Icon name="description" size={20} color="var(--bp-blue)" />
                <span data-phi="true">
                  {ctx.reasonText && ctx.reasonText.trim()
                    ? ctx.reasonText
                    : 'No reason was provided.'}
                </span>
              </div>
              {(detail.symptomCount > 0 || detail.severityScore > 0 || detail.symptomDurationCode) && (
                <div className="pg-meta-row" style={{ display: 'flex', gap: 28, marginTop: 12, flexWrap: 'wrap' }}>
                  {ctx.symptoms && ctx.symptoms.length > 0 && (
                    <KeyValue label="Symptoms" value={ctx.symptoms.join(' · ')} />
                  )}
                  {detail.symptomDurationCode && (
                    <KeyValue
                      label="Duration"
                      value={DURATION_LABEL[detail.symptomDurationCode] ?? detail.symptomDurationCode}
                    />
                  )}
                  {detail.severityScore > 0 && (
                    <KeyValue label="Severity" value={`${detail.severityScore} / 10`} />
                  )}
                </div>
              )}
            </Section>

            {/* Quick check / triage */}
            {ctx.quickCheck && ctx.quickCheck.length > 0 && (
              <Section label="Quick check">
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
                  {detail.redFlag ? (
                    <span className="wl-redflag">
                      <Icon name="emergency" size={15} fill />
                      Red flag
                    </span>
                  ) : (
                    <Pill tone="green" icon="check_circle">
                      No red flags
                    </Pill>
                  )}
                </div>
                {ctx.quickCheck.map((q, i) => (
                  <div key={i} className="pg-kv">
                    <span>{q.question}</span>
                    <span style={{ fontWeight: q.concerning ? 700 : 500, color: q.concerning ? 'var(--bp-danger)' : 'var(--bp-text)' }}>
                      {q.answer}
                    </span>
                  </div>
                ))}
              </Section>
            )}

            {/* Medical history */}
            {ctx.medicalHistory && ctx.medicalHistory.length > 0 && (
              <Section label={labels.context.history}>
                <ul
                  style={{ margin: 0, paddingLeft: 18, color: 'var(--bp-text-2)', fontSize: 15, lineHeight: 1.7 }}
                  data-phi="true"
                >
                  {ctx.medicalHistory.map((h, i) => (
                    <li key={i}>
                      <strong>{h.label}</strong>
                      {h.detail ? ` — ${h.detail}` : ''}
                    </li>
                  ))}
                </ul>
              </Section>
            )}

            {/* Previous encounters */}
            <Section label="Previous encounters">
              {ctx.previousEncounters && ctx.previousEncounters.length > 0 ? (
                ctx.previousEncounters.map((e, i) => (
                  <div key={i} className="pg-kv">
                    <span>
                      {e.date ? `${e.date} · ` : ''}
                      {e.diagnosis ?? ''}
                    </span>
                    <span style={{ color: 'var(--bp-muted)' }}>
                      {[e.physician, e.modality].filter(Boolean).join(' · ')}
                    </span>
                  </div>
                ))
              ) : (
                <span style={{ color: 'var(--bp-muted)' }}>No previous encounters recorded.</span>
              )}
            </Section>
          </>
        }
        rail={
          <>
            {/* Readiness — derived from the live worklist projection. */}
            <InfoCard title="Readiness">
              <CheckItem
                title="Identity"
                sub={detail.consultationStatus === 'SCHEDULED' ? 'Pending' : 'Verified'}
                done={detail.consultationStatus !== 'SCHEDULED'}
              />
              <CheckItem
                title="Consents"
                sub="At booking"
                done
              />
              <CheckItem
                title="Device check"
                sub={
                  detail.technicalStatus === 'IMPAIRED'
                    ? 'Needs attention'
                    : detail.readiness === 'READY'
                      ? 'Passed'
                      : 'In progress'
                }
                done={detail.readiness === 'READY' && detail.technicalStatus !== 'IMPAIRED'}
              />
              {status.detail && (
                <div className="pg-kv-note">{status.detail}</div>
              )}
            </InfoCard>

            {/* Payment — only when the event carried a fee. */}
            {detail.consultationFeeMinor > 0 && (
              <InfoCard
                title="Payment"
                accessory={
                  detail.paymentStatus === 'PAID' ? (
                    <Pill tone="green" icon="check_circle">
                      Paid
                    </Pill>
                  ) : (
                    <Pill tone="amber" icon="schedule">
                      {detail.paymentStatus === 'UNPAID' ? 'Unpaid' : (detail.paymentStatus ?? '—')}
                    </Pill>
                  )
                }
              >
                <KeyValue label="Consultation fee" value={money(detail.consultationFeeMinor, detail.currency)} />
                {detail.voucherMinor > 0 && (
                  <KeyValue label="Insurance voucher" value={`− ${money(detail.voucherMinor, detail.currency)}`} />
                )}
                <KeyValue label="Amount paid" value={money(detail.amountPaidMinor, detail.currency)} total />
              </InfoCard>
            )}

            {/* Attachments */}
            {ctx.attachments && ctx.attachments.length > 0 && (
              <InfoCard title="Attachments">
                {ctx.attachments.map((a, i) => (
                  <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '6px 0' }}>
                    <Icon name="image" size={18} color="var(--bp-blue)" />
                    <div style={{ minWidth: 0 }}>
                      <div style={{ fontSize: 14, color: 'var(--bp-text)' }}>{a.filename ?? 'Attachment'}</div>
                      <div style={{ fontSize: 12, color: 'var(--bp-muted)' }}>
                        {[a.sizeLabel, a.capturedAt].filter(Boolean).join(' · ')}
                      </div>
                    </div>
                  </div>
                ))}
              </InfoCard>
            )}
          </>
        }
      />
    </div>
  );
}
