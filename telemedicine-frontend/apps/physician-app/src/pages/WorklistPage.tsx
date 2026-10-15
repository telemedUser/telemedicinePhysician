import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { usePhysicianApi } from '../api/physicianApi';
import type { Readiness, WorklistEntry, WorklistFlag } from '../api/types';
import { MOCK_WORKLIST_AFTER } from '../data/mockData';
import { labels } from '../config/labels';
import {
  Avatar,
  BpButton,
  BpSelect,
  Card,
  Icon,
  Pill,
  SectionHeader,
  type Tone,
} from '../ui/primitives';
import { formatWaiting } from '../components/badges';

const REFRESH_MS = 15000;
const READINESS_RANK: Record<Readiness, number> = { READY: 0, IN_PROGRESS: 1, NOT_READY: 2 };
type SortKey = 'waiting' | 'readiness';

const READINESS_TONE: Record<Readiness, Tone> = {
  READY: 'green',
  IN_PROGRESS: 'amber',
  NOT_READY: 'neutral',
};
const FLAG_TONE: Record<WorklistFlag, Tone> = {
  FIRST_VISIT: 'blue',
  FOLLOW_UP: 'navy',
  GUEST: 'amber',
  RED_FLAG: 'red',
};

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

export function WorklistPage() {
  const api = usePhysicianApi();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const afterView = searchParams.get('view') === 'after';
  const [entries, setEntries] = useState<WorklistEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [sortKey, setSortKey] = useState<SortKey>('readiness');
  const [readyOnly, setReadyOnly] = useState(false);
  const [busy, setBusy] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (afterView) {
      setEntries(MOCK_WORKLIST_AFTER);
    } else {
      setEntries(await api.getWorklist());
    }
    setLoading(false);
  }, [api, afterView]);

  // Keep a ref to the latest load so the polling interval is created exactly once and never
  // re-subscribes when `api`/`load` identity changes between renders (which previously stacked
  // intervals and hammered the backend).
  const loadRef = useRef(load);
  loadRef.current = load;

  useEffect(() => {
    void loadRef.current();
    if (afterView) return;
    const timer = window.setInterval(() => void loadRef.current(), REFRESH_MS);
    return () => window.clearInterval(timer);
  }, [afterView]);

  const visible = useMemo(() => {
    let rows = [...entries];
    if (readyOnly) rows = rows.filter((e) => e.readiness === 'READY');
    rows.sort((a, b) =>
      sortKey === 'waiting'
        ? b.waitingSeconds - a.waitingSeconds
        : READINESS_RANK[a.readiness] - READINESS_RANK[b.readiness],
    );
    return rows;
  }, [entries, readyOnly, sortKey]);

  const readyCount = entries.filter((e) => e.readiness === 'READY').length;

  const admit = async (id: string) => {
    setBusy(id);
    try {
      await api.admitPatient(id);
      navigate(`/consultation/${id}`);
    } finally {
      setBusy(null);
    }
  };

  const acknowledge = async (id: string) => {
    setBusy(id);
    try {
      await api.acknowledgeRedFlag(id);
      setEntries((prev) =>
        prev.map((e) => (e.appointmentId === id ? { ...e, redFlagAcknowledged: true } : e)),
      );
    } finally {
      setBusy(null);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
      <SectionHeader
        title={afterView ? 'Worklist — after' : labels.worklist.title}
        subtitle={
          afterView
            ? 'After the consultation: completed and at-risk patients.'
            : `${entries.length} patients · ${readyCount} ready to see`
        }
        actions={
          afterView ? (
            <Link to="/worklist">
              <BpButton variant="secondary" icon="arrow_back">
                Active worklist
              </BpButton>
            </Link>
          ) : (
            <>
              <BpSelect
                aria-label={labels.worklist.sortBy}
                value={sortKey}
                onChange={(e) => setSortKey(e.target.value as SortKey)}
                style={{ width: 'auto' }}
              >
                <option value="readiness">{labels.worklist.sortReadiness}</option>
                <option value="waiting">{labels.worklist.sortWaiting}</option>
              </BpSelect>
              <BpButton
                variant={readyOnly ? 'subtle' : 'secondary'}
                icon={readyOnly ? 'filter_alt' : 'filter_alt_off'}
                onClick={() => setReadyOnly((v) => !v)}
              >
                {labels.worklist.filterReady}
              </BpButton>
              <BpButton variant="secondary" icon="refresh" onClick={() => void load()}>
                {labels.common.refresh}
              </BpButton>
            </>
          )
        }
      />

      {loading ? (
        <Card>
          <span style={{ color: 'var(--bp-muted)' }}>{labels.common.loading}</span>
        </Card>
      ) : visible.length === 0 ? (
        <Card style={{ textAlign: 'center', padding: 40 }}>
          <Icon name="inbox" size={32} color="var(--bp-muted-2)" />
          <p style={{ color: 'var(--bp-muted)', margin: '8px 0 0' }}>{labels.worklist.empty}</p>
        </Card>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {visible.map((entry) => {
            const hasRedFlag = entry.flags.includes('RED_FLAG');
            const needsAck = hasRedFlag && !entry.redFlagAcknowledged;
            const completed = entry.consultationStatus === 'CLOSED';
            const atRisk = entry.technicalStatus === 'IMPAIRED';
            const canAdmit = entry.readiness === 'READY' && !needsAck && !completed;
            return (
              <Card
                key={entry.appointmentId}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 16,
                  borderLeft: `4px solid ${
                    needsAck || atRisk
                      ? needsAck
                        ? 'var(--bp-danger)'
                        : 'var(--bp-warn)'
                      : completed
                        ? 'var(--bp-ok-2)'
                        : entry.readiness === 'READY'
                          ? 'var(--bp-ok-2)'
                          : 'var(--bp-border)'
                  }`,
                  opacity: completed ? 0.92 : 1,
                }}
              >
                <Avatar initials={initials(entry.patientRef)} tone={needsAck ? 'amber' : 'blue'} size={44} />

                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
                    <strong style={{ fontSize: 15 }} data-phi="true">
                      {entry.patientRef}
                    </strong>
                    <span style={{ color: 'var(--bp-muted-2)', fontSize: 12, fontFamily: 'ui-monospace, Menlo, monospace' }}>
                      {entry.appointmentId}
                    </span>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 7, flexWrap: 'wrap' }}>
                    {completed ? (
                      <Pill tone="green" icon="task_alt">
                        Completed · documents delivered
                      </Pill>
                    ) : (
                      <Pill tone={READINESS_TONE[entry.readiness]} dot>
                        {labels.readiness[entry.readiness]}
                      </Pill>
                    )}
                    <Pill tone="neutral" icon={entry.mode === 'audio' ? 'call' : 'videocam'}>
                      {entry.mode === 'audio' ? 'Audio' : 'Video'}
                    </Pill>
                    {entry.technicalStatus !== 'OK' && (
                      <Pill tone="red" icon="wifi_off">
                        {labels.technical[entry.technicalStatus]}
                      </Pill>
                    )}
                    {entry.telemetryAvailable && (
                      <Pill tone="blue" icon="monitor_heart">
                        Telemetry
                      </Pill>
                    )}
                    {entry.flags.map((flag) => (
                      <Pill key={flag} tone={FLAG_TONE[flag]} icon={flag === 'RED_FLAG' ? 'warning' : undefined}>
                        {labels.flags[flag]}
                      </Pill>
                    ))}
                  </div>
                </div>

                <div style={{ textAlign: 'right', flex: 'none' }}>
                  <div style={{ fontSize: 12, color: 'var(--bp-muted)' }}>{labels.worklist.columnWaiting}</div>
                  <div style={{ fontWeight: 700, fontFamily: 'ui-monospace, Menlo, monospace' }}>
                    {completed ? '—' : formatWaiting(entry.waitingSeconds)}
                  </div>
                </div>

                <div style={{ display: 'flex', gap: 8, flex: 'none' }}>
                  {completed ? (
                    <Link to={`/consultation/${entry.appointmentId}/review`}>
                      <BpButton variant="secondary" icon="description">
                        Summary
                      </BpButton>
                    </Link>
                  ) : needsAck ? (
                    <BpButton variant="danger" icon="warning" loading={busy === entry.appointmentId} onClick={() => void acknowledge(entry.appointmentId)}>
                      {labels.worklist.acknowledgeRedFlag}
                    </BpButton>
                  ) : atRisk ? (
                    <BpButton variant="secondary" icon="help" onClick={() => void admit(entry.appointmentId)}>
                      Send device help
                    </BpButton>
                  ) : (
                    <BpButton icon="login" disabled={!canAdmit} loading={busy === entry.appointmentId} onClick={() => void admit(entry.appointmentId)}>
                      {labels.worklist.admit}
                    </BpButton>
                  )}
                  {!completed && (
                    <Link to={`/worklist/${entry.appointmentId}/context`}>
                      <BpButton variant="secondary" icon="person">
                        {labels.worklist.context}
                      </BpButton>
                    </Link>
                  )}
                </div>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
}
