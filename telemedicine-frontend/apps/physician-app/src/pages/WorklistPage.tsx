import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { usePhysicianApi } from '../api/physicianApi';
import type { Readiness, WorklistEntry } from '../api/types';
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

/** Entered-at as a HH:MM clock, falling back to a dash when the patient hasn't checked in. */
function clockOf(entry: WorklistEntry): string {
  if (!entry.enteredAt) return '—';
  const d = new Date(entry.enteredAt);
  if (Number.isNaN(d.getTime())) return '—';
  return d.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
}

/** "Waiting 8 min" style, matching the reference copy. */
function waitingLabel(seconds: number): string {
  const mins = Math.max(0, Math.round(seconds / 60));
  if (mins < 1) return 'Waiting <1 min';
  return `Waiting ${mins} min`;
}

function isCompleted(e: WorklistEntry) {
  return e.consultationStatus === 'CLOSED';
}

export function WorklistPage() {
  const api = usePhysicianApi();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const afterView = searchParams.get('view') === 'after';
  const [entries, setEntries] = useState<WorklistEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState<TabKey>('all');
  const [busy, setBusy] = useState<string | null>(null);

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

  const counts = useMemo(() => {
    const completed = entries.filter(isCompleted).length;
    const waiting = entries.filter((e) => e.readiness === 'READY' && !isCompleted(e)).length;
    return { all: entries.length, waiting, completed };
  }, [entries]);

  const visible = useMemo(() => {
    let rows = [...entries];
    if (tab === 'waiting') rows = rows.filter((e) => e.readiness === 'READY' && !isCompleted(e));
    else if (tab === 'completed') rows = rows.filter(isCompleted);
    // "Sorted by time": by scheduled/entered time ascending, unknown times last.
    rows.sort((a, b) => {
      const ta = a.enteredAt ? Date.parse(a.enteredAt) : Number.POSITIVE_INFINITY;
      const tb = b.enteredAt ? Date.parse(b.enteredAt) : Number.POSITIVE_INFINITY;
      if (ta !== tb) return ta - tb;
      return READINESS_RANK[a.readiness] - READINESS_RANK[b.readiness];
    });
    return rows;
  }, [entries, tab]);

  const admit = async (id: string) => {
    setBusy(id);
    try {
      await api.admitPatient(id);
      navigate(`/consultation/${id}`);
    } finally {
      setBusy(null);
    }
  };

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
                const hasRedFlag = entry.flags.includes('RED_FLAG');
                const completed = isCompleted(entry);
                const atRisk = entry.technicalStatus === 'IMPAIRED';
                const canAdmit = entry.readiness === 'READY' && !completed;
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
                      <StatusCell entry={entry} completed={completed} atRisk={atRisk} />
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
                            disabled={busy === entry.appointmentId}
                            onClick={() => void admit(entry.appointmentId)}
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
                          onClick={() => navigate(`/worklist/${entry.appointmentId}/context`)}
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

/** The Status column: a subtle pill plus a muted sub-line (e.g. "Waiting 8 min"). */
function StatusCell({
  entry,
  completed,
  atRisk,
}: {
  entry: WorklistEntry;
  completed: boolean;
  atRisk: boolean;
}) {
  if (completed) {
    return (
      <>
        <span className="wl-chip wl-chip-green">
          <Icon name="task_alt" size={14} />
          Completed
        </span>
        <div className="wl-substatus">Docs delivered</div>
      </>
    );
  }
  if (entry.consultationStatus === 'OPEN_DOCTOR' || entry.consultationStatus === 'STARTED') {
    return (
      <>
        <span className="wl-chip wl-chip-blue">
          <Icon name="videocam" size={14} />
          In consultation
        </span>
        {entry.enteredAt && (
          <div className="wl-substatus">
            Since{' '}
            {new Date(entry.enteredAt).toLocaleTimeString('en-GB', {
              hour: '2-digit',
              minute: '2-digit',
            })}
          </div>
        )}
      </>
    );
  }
  if (atRisk) {
    return (
      <>
        <span className="wl-chip wl-chip-red">
          <Icon name="wifi_off" size={14} />
          {labels.technical[entry.technicalStatus]}
        </span>
        <div className="wl-substatus">Device check needed</div>
      </>
    );
  }
  if (entry.readiness === 'READY') {
    return (
      <>
        <span className="wl-chip wl-chip-green">
          <Icon name="check_circle" size={14} />
          Ready
        </span>
        <div className="wl-substatus">{waitingLabel(entry.waitingSeconds)}</div>
      </>
    );
  }
  if (entry.readiness === 'IN_PROGRESS') {
    return (
      <span className="wl-chip wl-chip-amber">
        <Icon name="hourglass_top" size={14} />
        Preparing
      </span>
    );
  }
  return (
    <span className="wl-chip wl-chip-neutral">
      <Icon name="schedule" size={14} />
      Not arrived
    </span>
  );
}
