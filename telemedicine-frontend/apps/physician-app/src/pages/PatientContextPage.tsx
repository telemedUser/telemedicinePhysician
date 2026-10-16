import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
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
import { MOCK_CONTEXTS } from '../data/mockData';

function initials(name: string) {
  return name
    .replace(/\(.*?\)/g, '')
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((p) => p[0]?.toUpperCase() ?? '')
    .join('');
}

const inr = (n?: number | null) =>
  n == null
    ? '—'
    : new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(n);

function fmtDateTime(iso?: string) {
  if (!iso) return '—';
  const d = new Date(iso);
  return Number.isNaN(d.getTime())
    ? iso
    : d.toLocaleString('en-GB', {
        weekday: 'short',
        day: 'numeric',
        month: 'short',
        hour: '2-digit',
        minute: '2-digit',
      });
}

/** Shape returned by the patient-booking-service GET /api/appointments/{reference}. */
interface BookingDetail {
  reference: string;
  status: string;
  careSubjectId?: string;
  doctor?: { id: string; name: string; specialty: string } | null;
  slot?: { id: string; startsAt: string; mode: string } | null;
  mode?: string;
  reason?: string | null;
  feeClp?: number | null;
  createdAt?: string;
}

/**
 * "Before you join" patient context (reached from the worklist "View details"). For a real patient
 * booking it shows exactly what the patient submitted at booking time — reason for visit, chosen
 * doctor, scheduled slot, mode and fee — read live from the patient-booking-service. For the demo
 * mock ids it falls back to the deck-derived clinical context.
 */
export function PatientContextPage() {
  const { appointmentId = '' } = useParams<{ appointmentId: string }>();
  const navigate = useNavigate();
  const mock = MOCK_CONTEXTS[appointmentId];

  const [booking, setBooking] = useState<BookingDetail | null>(null);
  const [state, setState] = useState<'loading' | 'booking' | 'mock' | 'empty'>('loading');

  useEffect(() => {
    let active = true;
    void (async () => {
      try {
        const res = await fetch(`/booking-api/appointments/${encodeURIComponent(appointmentId)}`);
        if (!active) return;
        if (res.ok) {
          setBooking((await res.json()) as BookingDetail);
          setState('booking');
          return;
        }
      } catch {
        /* fall through to mock / empty */
      }
      if (!active) return;
      setState(mock ? 'mock' : 'empty');
    })();
    return () => {
      active = false;
    };
  }, [appointmentId, mock]);

  if (state === 'loading') {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
        <PageBack label={labels.nav.worklist} onClick={() => navigate('/worklist')} />
        <div style={{ padding: 24, color: 'var(--bp-muted)' }}>{labels.common.loading}</div>
      </div>
    );
  }

  /* ---------- Real booking details (what the patient submitted) ---------- */
  if (state === 'booking' && booking) {
    const name = booking.careSubjectId ?? appointmentId;
    const doctor = booking.doctor;
    const slot = booking.slot;
    const mode = booking.mode ?? slot?.mode ?? 'video';
    const cancelled = booking.status === 'cancelled';
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
        <PageBack label={labels.nav.worklist} onClick={() => navigate('/worklist')} />

        <PageHeader
          initials={initials(name)}
          name={name}
          statusPill={
            cancelled ? (
              <Pill tone="red" icon="cancel">
                Cancelled
              </Pill>
            ) : (
              <Pill tone="green" icon="event_available">
                Booked
              </Pill>
            )
          }
          meta={
            <>
              <span>
                <Icon name="schedule" size={15} />
                {fmtDateTime(slot?.startsAt)}
              </span>
              <span>
                <Icon name={mode === 'audio' ? 'call' : 'videocam'} size={15} />
                {mode === 'audio' ? 'Audio' : 'Video'}
              </span>
              <span style={{ fontFamily: 'ui-monospace, Menlo, monospace' }}>{booking.reference}</span>
            </>
          }
          banner={{
            icon: 'assignment_ind',
            title: 'Booking details',
            sub: <span>Submitted by the patient at booking</span>,
          }}
        />

        <PageBody
          main={
            <>
              <Section label="Reason for visit">
                <div className="pg-section-lead">
                  <Icon name="description" size={20} color="var(--bp-blue)" />
                  <span data-phi="true">
                    {booking.reason && booking.reason.trim()
                      ? booking.reason
                      : 'No reason was provided at booking.'}
                  </span>
                </div>
              </Section>

              <Section label="Appointment">
                <div className="pg-section-lead">
                  <Icon name="event" size={20} color="var(--bp-blue)" />
                  <span>
                    <strong>{fmtDateTime(slot?.startsAt)}</strong> · {mode === 'audio' ? 'Audio' : 'Video'} consultation
                  </span>
                </div>
              </Section>
            </>
          }
          rail={
            <>
              <InfoCard title="Booked with">
                <div style={{ fontWeight: 600, color: 'var(--bp-text)' }}>{doctor?.name ?? '—'}</div>
                <div style={{ fontSize: 13, color: 'var(--bp-muted)', marginTop: 2 }}>
                  {doctor?.specialty ?? ''}
                </div>
              </InfoCard>

              <InfoCard
                title="Booking"
                accessory={
                  cancelled ? (
                    <Pill tone="red" icon="cancel">
                      Cancelled
                    </Pill>
                  ) : (
                    <Pill tone="green" icon="check_circle">
                      Confirmed
                    </Pill>
                  )
                }
              >
                <KeyValue label="Reference" value={booking.reference} />
                <KeyValue label="Mode" value={mode === 'audio' ? 'Audio' : 'Video'} />
                <KeyValue label="Consultation fee" value={inr(booking.feeClp)} total />
                <div className="pg-kv-note">Booked {fmtDateTime(booking.createdAt)}</div>
              </InfoCard>

              {!cancelled && (
                <BpButton
                  icon="login"
                  style={{ width: '100%' }}
                  onClick={() => navigate(`/consultation/${appointmentId}`)}
                >
                  {labels.worklist.admit}
                </BpButton>
              )}
            </>
          }
        />
      </div>
    );
  }

  /* ---------- Demo clinical context (mock ids) ---------- */
  if (state === 'mock' && mock) {
    const ctx = mock;
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
        <PageBack label={labels.nav.worklist} onClick={() => navigate('/worklist')} />

        <PageHeader
          initials={initials(ctx.name)}
          name={ctx.name}
          statusPill={
            ctx.redFlag ? (
              <span className="wl-redflag">
                <Icon name="emergency" size={16} fill />
                {labels.flags.RED_FLAG}
              </span>
            ) : (
              <Pill tone="blue" icon="lock">
                Identity verified
              </Pill>
            )
          }
          meta={
            <>
              <span>
                <Icon name="cake" size={15} />
                {ctx.age} yrs · {ctx.sex}
              </span>
              <span>
                <Icon name="shield" size={15} />
                {ctx.insurance}
              </span>
            </>
          }
          banner={{
            icon: ctx.redFlag ? 'warning' : 'login',
            title: ctx.redFlag ? 'Red flag — review before joining' : 'Ready to join',
            sub: ctx.redFlag ? <span>{ctx.redFlag}</span> : <span>Consents captured · identity verified</span>,
          }}
        />

        <PageBody
          main={
            <>
              <Section label="Reason for consultation">
                <div className="pg-section-lead">
                  <Icon name="description" size={20} color="var(--bp-blue)" />
                  <span data-phi="true">{ctx.reason}</span>
                </div>
              </Section>
              <ListSection label={labels.context.medications} items={ctx.medications} />
              <ListSection label={labels.context.allergies} items={ctx.allergies} />
              <ListSection label={labels.context.history} items={ctx.conditions} />
            </>
          }
          rail={
            <>
              <InfoCard title="Readiness">
                <CheckItem title="Platform consent" sub="Captured" />
                <CheckItem title="Clinical consent" sub="Captured" />
                <CheckItem title="Identity" sub="Verified" />
              </InfoCard>
              <BpButton icon="login" style={{ width: '100%' }} onClick={() => navigate(`/consultation/${appointmentId}`)}>
                {labels.worklist.admit}
              </BpButton>
            </>
          }
        />
      </div>
    );
  }

  /* ---------- Nothing found ---------- */
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
      <PageBack label={labels.nav.worklist} onClick={() => navigate('/worklist')} />
      <div className="pg-infocard" style={{ textAlign: 'center', padding: 40 }}>
        <Icon name="cloud_off" size={32} color="var(--bp-muted-2)" />
        <p style={{ color: 'var(--bp-muted)' }}>
          No booking details are available for this appointment yet.
        </p>
      </div>
    </div>
  );
}

/** List block inside the left column: an overline section with a bulleted list. */
function ListSection({ label, items }: { label: string; items: string[] }) {
  return (
    <Section label={label}>
      {items.length === 0 ? (
        <span style={{ color: 'var(--bp-muted)' }}>{labels.context.none}</span>
      ) : (
        <ul style={{ margin: 0, paddingLeft: 18, color: 'var(--bp-text-2)', fontSize: 15, lineHeight: 1.7 }} data-phi="true">
          {items.map((it, i) => (
            <li key={i}>{it}</li>
          ))}
        </ul>
      )}
    </Section>
  );
}
