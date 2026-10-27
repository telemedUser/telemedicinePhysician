import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { usePhysicianApi } from '../api/physicianApi';
import type { WorklistEntry } from '../api/types';
import { deriveWorklistStatus, isCompleted } from '../api/worklistStatus';
import { Icon } from '../ui/primitives';
import { getPhysicianSession } from '../auth/physicianSession';

const REFRESH_MS = 15000;

function greeting(): string {
  const h = new Date().getHours();
  if (h < 12) return 'Good morning';
  if (h < 17) return 'Good afternoon';
  return 'Good evening';
}

function shortName(displayName?: string): string {
  if (!displayName) return 'Doctor';
  // "Dr. Arjun Khanna" -> "Dr. Khanna"
  const m = displayName.match(/^(Dr\.?|Dra\.?)\s+(.+)$/i);
  if (m) {
    const parts = m[2].split(/\s+/);
    return `Dr. ${parts[parts.length - 1]}`;
  }
  return displayName;
}

function clockOf(iso?: string): string {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '—';
  // Date + time, e.g. "22 Oct, 16:20", so the clinic reads when as well as what time.
  return d.toLocaleString('en-GB', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' });
}

/** Map the shared chip colour to an icon/text colour for the Home "Today" table. */
const CHIP_COLOR: Record<string, string> = {
  green: 'var(--bp-ok)',
  blue: 'var(--bp-info)',
  amber: 'var(--bp-warn)',
  red: 'var(--bp-danger)',
  neutral: 'var(--bp-muted)',
};

/**
 * In-person HIS sessions shown on the Today timeline. These are owned by the hospital information
 * system (HIS), a separate platform we do not build — so they are hardcoded here as placeholders
 * for what would be an internal HIS API. Only the Virtual clinic entry below is live (worklist).
 */
const HIS_SESSIONS = [
  {
    time: '09:00–13:00',
    title: 'Morning clinic',
    meta: 'In person · Box 12 · 14 patients',
    note: 'All encounters documented and signed.',
    status: 'Completed',
  },
  {
    time: '14:00–15:30',
    title: 'Ward round',
    meta: 'Inpatients · 3rd floor · 5 patients',
    note: 'Notes signed · 1 discharge planned',
    status: 'Completed',
  },
] as const;

/** A completed/active marker dot for the timeline rail. */
function TimelineDot({ active }: { active?: boolean }) {
  return active ? (
    <span
      style={{
        width: 14,
        height: 14,
        borderRadius: '50%',
        border: '3px solid var(--bp-info)',
        background: '#fff',
        flex: 'none',
        marginTop: 2,
      }}
    />
  ) : (
    <Icon name="check_circle" size={16} color="var(--bp-ok)" fill />
  );
}

/**
 * Physician home / dashboard (prototype D-01), scoped to the signed-in doctor. The Today timeline
 * lists the day's sessions: the in-person HIS sessions (Morning clinic, Ward round) are hardcoded
 * placeholders for internal HIS APIs, while the Virtual clinic is live — its table is synced with
 * the worklist (GET /api/v1/physicians/me/worklist, X-Physician-Id). The per-patient Time is the
 * patient's booked consultation time (scheduledAt) from the patient service.
 */
export function HomePage() {
  const api = usePhysicianApi();
  const navigate = useNavigate();
  const session = getPhysicianSession();
  const [entries, setEntries] = useState<WorklistEntry[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    const load = () => {
      void api.getWorklist().then((rows) => {
        if (active) {
          setEntries(rows);
          setLoading(false);
        }
      });
    };
    load();
    const t = window.setInterval(load, REFRESH_MS);
    return () => {
      active = false;
      window.clearInterval(t);
    };
  }, [api]);

  // Sort by booked consultation time (scheduledAt) so the clinic reads top-to-bottom like the
  // prototype; fall back to waiting-room arrival, then unknown last.
  const rows = useMemo(() => {
    const timeOf = (e: WorklistEntry) =>
      e.scheduledAt ? Date.parse(e.scheduledAt) : e.enteredAt ? Date.parse(e.enteredAt) : Number.POSITIVE_INFINITY;
    return [...entries].sort((a, b) => timeOf(a) - timeOf(b));
  }, [entries]);

  const total = rows.length;
  // "In session" when anyone is live or waiting/ready — derived from the same shared status.
  const inSession = rows.some((e) => {
    const k = deriveWorklistStatus(e).key;
    return k === 'IN_CONSULTATION' || k === 'READY' || k === 'IN_WAITING_ROOM';
  });

  // One ready patient highlighted on the Virtual clinic timeline line, like the prototype.
  const readyPatient = rows.find((e) => deriveWorklistStatus(e).key === 'READY');
  const readyWaiting = readyPatient ? deriveWorklistStatus(readyPatient).detail : '';

  // Sessions count = the two HIS sessions + the virtual clinic; patient total across all.
  const sessionCount = HIS_SESSIONS.length + 1;
  const hisPatients = 14 + 5; // from the hardcoded HIS sessions above
  const patientTotal = hisPatients + total;

  // Lightweight inbox summary (dashboard context; not part of the virtual-consult core data).
  const inbox = [
    { icon: 'lab_research', label: 'Results to review', sub: 'Lab · no critical values', count: 2 },
    { icon: 'edit_note', label: 'Notes to sign', sub: 'Ward round · bed 312', count: 1 },
    { icon: 'prescriptions', label: 'Prescription renewals', sub: 'Pending approval', count: 1 },
    { icon: 'forum', label: 'Messages', sub: '2 nursing · 1 pharmacy', count: 3 },
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
      <h1 style={{ margin: 0, fontSize: 24, fontWeight: 700, color: 'var(--bp-navy)' }}>
        {greeting()}, {shortName(session?.displayName)}
      </h1>

      <div style={{ display: 'grid', gridTemplateColumns: 'minmax(0, 1fr) 320px', gap: 24, alignItems: 'start' }} className="home-grid">
        {/* Today — timeline of sessions (HIS hardcoded + live Virtual clinic) */}
        <div className="pg-infocard" style={{ background: 'var(--bp-surface)', padding: 0, overflow: 'hidden' }}>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 10, padding: '16px 18px 6px' }}>
            <strong style={{ fontSize: 16, color: 'var(--bp-text)' }}>Today</strong>
            <span style={{ fontSize: 13, color: 'var(--bp-muted)' }}>
              {sessionCount} sessions · {patientTotal} patients
            </span>
          </div>

          <div className="home-timeline" style={{ padding: '6px 18px 4px' }}>
            {/* HIS sessions — hardcoded (internal HIS APIs, not built here). */}
            {HIS_SESSIONS.map((s) => (
              <div key={s.title} className="home-tl-row" style={{ display: 'flex', gap: 12, padding: '10px 0' }}>
                <TimelineDot />
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ display: 'flex', alignItems: 'baseline', gap: 10, flexWrap: 'wrap' }}>
                    <span style={{ fontVariantNumeric: 'tabular-nums', fontWeight: 600, color: 'var(--bp-text)' }}>
                      {s.time}
                    </span>
                    <strong style={{ color: 'var(--bp-text)' }}>{s.title}</strong>
                    <span style={{ fontSize: 13, color: 'var(--bp-muted)' }}>{s.meta}</span>
                    <span style={{ flex: 1 }} />
                    <span className="wl-chip wl-chip-green" style={{ fontSize: 12 }}>
                      <Icon name="task_alt" size={14} />
                      {s.status}
                    </span>
                  </div>
                  <div style={{ fontSize: 13, color: 'var(--bp-muted)', marginTop: 2 }}>{s.note}</div>
                </div>
              </div>
            ))}

            {/* Virtual clinic — live. */}
            <div className="home-tl-row" style={{ display: 'flex', gap: 12, padding: '10px 0 4px' }}>
              <TimelineDot active />
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: 'flex', alignItems: 'baseline', gap: 10, flexWrap: 'wrap' }}>
                  <strong style={{ color: 'var(--bp-text)' }}>Virtual clinic</strong>
                  <span style={{ fontSize: 13, color: 'var(--bp-muted)' }}>
                    Connected Care Telemedicine · {total} {total === 1 ? 'patient' : 'patients'}
                  </span>
                  <span style={{ flex: 1 }} />
                  <span
                    className={inSession ? 'wl-chip wl-chip-blue' : 'wl-chip wl-chip-neutral'}
                    style={{ fontSize: 12 }}
                  >
                    {inSession ? 'In session' : 'Scheduled'}
                  </span>
                </div>
                {readyPatient && (
                  <div style={{ fontSize: 13, color: 'var(--bp-muted)', marginTop: 2 }}>
                    1 patient ready · {readyPatient.patientRef} {clockOf(readyPatient.scheduledAt)}
                    {readyWaiting ? ` · ${readyWaiting.replace(/^Arrived \S+ · /, '')}` : ''}
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Virtual-clinic patient table (synced with the worklist) */}
          <div style={{ padding: '4px 18px 0' }}>
            {loading ? (
              <div style={{ padding: 18, color: 'var(--bp-muted)' }}>Loading today's clinic…</div>
            ) : rows.length === 0 ? (
              <div style={{ padding: 18, color: 'var(--bp-muted)' }}>No one scheduled yet.</div>
            ) : (
              <table className="wl-table" style={{ width: '100%' }}>
                <thead>
                  <tr>
                    <th>Time</th>
                    <th>Patient</th>
                    <th>Type</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((e) => {
                    const st = deriveWorklistStatus(e);
                    const color = CHIP_COLOR[st.chip];
                    return (
                      <tr key={e.appointmentId}>
                        {/* Time = the patient's booked consultation time from the patient service. */}
                        <td className="wl-time">{clockOf(e.scheduledAt)}</td>
                        <td data-phi="true">{e.patientRef ?? '—'}</td>
                        <td>{e.mode === 'audio' ? 'Audio' : 'Video'}</td>
                        <td>
                          <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6, color, fontSize: 13, fontWeight: 600 }}>
                            <Icon name={st.icon} size={15} color={color} />
                            {st.label}
                          </span>
                          {st.detail && (
                            <div style={{ fontSize: 12, fontWeight: 400, color: 'var(--bp-muted)', marginTop: 2 }}>
                              {st.detail}
                            </div>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            )}
          </div>

          <div style={{ padding: '14px 18px' }}>
            <button
              type="button"
              className="wl-btn wl-btn-primary"
              onClick={() => navigate('/worklist')}
              style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}
            >
              <Icon name="videocam" size={18} color="#fff" />
              Open virtual consultations
            </button>
          </div>
        </div>

        {/* Right rail — Inbox + Inpatients (HIS context, hardcoded) */}
        <div className="pg-rail">
          <div className="pg-infocard">
            <div className="pg-infocard-head">
              <span className="pg-infocard-title">Inbox</span>
              <span style={{ fontSize: 12, color: 'var(--bp-muted)' }}>{inbox.reduce((n, i) => n + i.count, 0)} items</span>
            </div>
            {inbox.map((i, idx) => (
              <div
                key={i.label}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 12,
                  padding: '10px 0',
                  borderTop: idx === 0 ? 'none' : '1px solid var(--bp-border-2)',
                }}
              >
                <Icon name={i.icon} size={20} color="var(--bp-blue)" />
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 14, fontWeight: 600, color: 'var(--bp-text)' }}>{i.label}</div>
                  <div style={{ fontSize: 12, color: 'var(--bp-muted)' }}>{i.sub}</div>
                </div>
                <span
                  style={{
                    flex: 'none',
                    minWidth: 22,
                    height: 22,
                    borderRadius: 999,
                    background: '#eef1f4',
                    color: 'var(--bp-muted)',
                    display: 'grid',
                    placeItems: 'center',
                    fontSize: 12,
                    fontWeight: 700,
                    padding: '0 6px',
                  }}
                >
                  {i.count}
                </span>
              </div>
            ))}
          </div>

          {/* Inpatients — HIS context (hardcoded placeholder for an internal HIS API). */}
          <div className="pg-infocard">
            <div className="pg-infocard-head">
              <span className="pg-infocard-title">Inpatients</span>
              <span style={{ fontSize: 12, color: 'var(--bp-muted)' }}>3rd floor</span>
            </div>
            <div style={{ fontSize: 20, fontWeight: 700, color: 'var(--bp-text)' }}>5 patients</div>
            <div style={{ fontSize: 12, color: 'var(--bp-muted)', marginTop: 4, lineHeight: 1.5 }}>
              1 discharge planned · no critical results overnight.
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
