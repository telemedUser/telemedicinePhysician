import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { usePhysicianApi } from '../api/physicianApi';
import type { Readiness, WorklistEntry } from '../api/types';
import { deriveWorklistStatus, hasArrived, isCompleted } from '../api/worklistStatus';
import { MOCK_WORKLIST_AFTER } from '../data/mockData';
import { labels } from '../config/labels';
import { Avatar, BpButton, Icon } from '../ui/primitives';

const REFRESH_MS = 15000;
const READINESS_RANK: Record<Readiness, number> = { READY: 0, IN_PROGRESS: 1, NOT_READY: 2 };
type TabKey = 'all' | 'waiting' | 'completed';

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

/** The patient's actual booked consultation time (scheduledAt) as HH:MM; falls back to waiting-room
 *  arrival only if the schedule is unknown, else a dash. */
function clockOf(entry: WorklistEntry): string {
  const iso = entry.scheduledAt ?? entry.enteredAt;
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '—';
  return d.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
}

export function WorklistPage() {
  const api = usePhysicianApi();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const afterView = searchParams.get('view') === 'after';
  const [entries, setEntries] = useState<WorklistEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState<TabKey>('all');

  const load = useCallback(async () => {
    if (afterView) {
      setEntries(MOCK_WORKLIST_AFTER);
    } else {
      setEntries(await api.getWorklist());
    }
    setLoading(false);
  }, [api, afterView]);

  const loadRef = useRef(load);
  loadRef.current = load;

  useEffect(() => {
    void loadRef.current();
    if (afterView) return;
    const timer = window.setInterval(() => void loadRef.current(), REFRESH_MS);
    return () => window.clearInterval(timer);
  }, [afterView]);

  // "Waiting" = patient has arrived in the waiting room and the consult isn't live/closed yet.
  const isWaiting = useCallback(
    (e: WorklistEntry) =>
      hasArrived(e) &&
      !isCompleted(e) &&
      e.consultationStatus !== 'OPEN_DOCTOR' &&
      e.consultationStatus !== 'STARTED',
    [],
  );

  const counts = useMemo(() => {
    const completed = entries.filter(isCompleted).length;
    const waiting = entries.filter(isWaiting).length;
    return { all: entries.length, waiting, completed };
  }, [entries, isWaiting]);

  const visible = useMemo(() => {
    let rows = [...entries];
    if (tab === 'waiting') rows = rows.filter(isWaiting);
    else if (tab === 'completed') rows = rows.filter(isCompleted);
    // "Sorted by time": by the booked consultation time (scheduledAt) ascending, unknown last.
    rows.sort((a, b) => {
      const sa = a.scheduledAt ?? a.enteredAt;
      const sb = b.scheduledAt ?? b.enteredAt;
      const ta = sa ? Date.parse(sa) : Number.POSITIVE_INFINITY;
      const tb = sb ? Date.parse(sb) : Number.POSITIVE_INFINITY;
      if (ta !== tb) return ta - tb;
      return READINESS_RANK[a.readiness] - READINESS_RANK[b.readiness];
    });
    return rows;
  }, [entries, tab, isWaiting]);

  const tabs: { key: TabKey; label: string; count: number }[] = [
    { key: 'all', label: 'All', count: counts.all },
    { key: 'waiting', label: 'Waiting', count: counts.waiting },
    { key: 'completed', label: 'Completed', count: counts.completed },
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      {/* Toolbar: tabs (left) + sort (right), sitting above the table panel. */}
      <div className="wl-toolbar">
        {afterView ? (
          <Link to="/worklist">
            <BpButton variant="secondary" icon="arrow_back">
              Active worklist
            </BpButton>
          </Link>
        ) : (
          <div className="wl-tabs" role="tablist" aria-label="Filter worklist">
            {tabs.map((t) => (
              <button
                key={t.key}
                role="tab"
                aria-selected={tab === t.key}
                className="wl-tab"
                onClick={() => setTab(t.key)}
              >
                {t.label}
                <span className="wl-tab-count">{t.count}</span>
              </button>
            ))}
          </div>
        )}
        <span className="wl-sort">
          <Icon name="swap_vert" size={16} />
          Sorted by time
        </span>
      </div>

      <div className="wl-panel">
        {loading ? (
          <div style={{ padding: 24, color: 'var(--bp-muted)' }}>{labels.common.loading}</div>
        ) : visible.length === 0 ? (
          <div style={{ padding: 40, textAlign: 'center' }}>
            <Icon name="inbox" size={32} color="var(--bp-muted-2)" />
            <p style={{ color: 'var(--bp-muted)', margin: '8px 0 0' }}>{labels.worklist.empty}</p>
          </div>
        ) : (
          <table className="wl-table">
            <thead>
              <tr>
                <th>Time</th>
                <th>Patient</th>
                <th>Status</th>
                <th>Flags</th>
                <th aria-label="Actions" />
              </tr>
            </thead>
            <tbody>
              {visible.map((entry) => {
                const status = deriveWorklistStatus(entry);
                const hasRedFlag = entry.flags.includes('RED_FLAG');
                const completed = status.key === 'COMPLETED';
                // Offer "Review & join" as soon as the patient is in the waiting room (arrived),
                // not only when device checks report READY.
                const canAdmit = status.arrived && !completed;
                const otherFlags = entry.flags.filter((f) => f !== 'RED_FLAG');

                return (
                  <tr key={entry.appointmentId} className={canAdmit ? 'is-active' : undefined}>
                    <td>
                      <div className="wl-time">{clockOf(entry)}</div>
                      <small className="wl-time">
                        <Icon name={entry.mode === 'audio' ? 'call' : 'videocam'} size={14} />
                        {entry.mode === 'audio' ? 'Audio' : 'Video'}
                      </small>
                    </td>

                    <td>
                      <div className="wl-patient">
                        <Avatar initials={initials(entry.patientRef)} tone="blue" size={38} />
                        <div style={{ minWidth: 0 }}>
                          <div className="wl-patient-name" data-phi="true">
                            {entry.patientRef}
                          </div>
                          <div className="wl-patient-id">{entry.appointmentId}</div>
                        </div>
                      </div>
                    </td>

                    <td>
                      <StatusCell status={status} />
                    </td>

                    <td>
                      <div className="wl-flagcell">
                        {hasRedFlag && (
                          <span className="wl-redflag">
                            <Icon name="emergency" size={16} fill />
                            {labels.flags.RED_FLAG}
                          </span>
                        )}
                        {otherFlags.map((flag) => (
                          <span key={flag} className="wl-flag">
                            {labels.flags[flag]}
                          </span>
                        ))}
                        {!hasRedFlag && otherFlags.length === 0 && (
                          <span style={{ color: 'var(--bp-muted-2)' }} />
                        )}
                      </div>
                    </td>

                    <td className="wl-actioncell">
                      {completed ? (
                        <button
                          type="button"
                          className="wl-btn"
                          onClick={() => navigate(`/consultation/${entry.appointmentId}/summary`)}
                        >
                          View summary
                        </button>
                      ) : canAdmit ? (
                        <>
                          <button
                            type="button"
                            className="wl-btn wl-btn-primary"
                            onClick={() => navigate(`/worklist/${entry.appointmentId}/review`)}
                          >
                            Review &amp; join
                          </button>
                          <button
                            type="button"
                            className="wl-link"
                            onClick={() => navigate(`/worklist/${entry.appointmentId}/context`)}
                          >
                            <Icon name="schedule" size={15} />
                            Notify delay
                          </button>
                        </>
                      ) : (
                        <button
                          type="button"
                          className="wl-btn"
                          onClick={() => navigate(`/worklist/${entry.appointmentId}/review`)}
                        >
                          View details
                        </button>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}

/**
 * The Status column: a subtle pill plus an optional muted sub-line. The status is derived from the
 * patient's real worklist data (shared with the Home page via deriveWorklistStatus), never
 * hard-coded per screen.
 */
function StatusCell({ status }: { status: ReturnType<typeof deriveWorklistStatus> }) {
  return (
    <>
      <span className={`wl-chip wl-chip-${status.chip}`}>
        <Icon name={status.icon} size={14} />
        {status.label}
      </span>
      {status.detail && <div className="wl-substatus">{status.detail}</div>}
    </>
  );
}
