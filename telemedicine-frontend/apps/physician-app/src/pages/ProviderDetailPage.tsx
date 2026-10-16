import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { usePhysicianApi } from '../api/physicianApi';
import type { ProviderDetail, ProviderStatus } from '../api/types';
import { labels } from '../config/labels';
import {
  Avatar,
  BpButton,
  BpInput,
  BpSelect,
  Card,
  Field,
  Icon,
  Overline,
  Pill,
  SectionHeader,
  type Tone,
} from '../ui/primitives';

const STATUSES: ProviderStatus[] = ['ACTIVE', 'SUSPENDED', 'ON_LEAVE', 'OFFBOARDED'];
const DAYS = ['', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
const STATUS_TONE: Record<ProviderStatus, Tone> = {
  ACTIVE: 'green',
  ON_LEAVE: 'amber',
  SUSPENDED: 'red',
  OFFBOARDED: 'neutral',
};

const pct = (v?: number) => (v === undefined ? '—' : `${Math.round(v * 100)}%`);
const mins = (s?: number) => (s === undefined ? '—' : `${Math.round(s / 60)} min`);
const initials = (name: string) =>
  name.replace(/^Dr[a]?\.\s*/i, '').split(/\s+/).slice(0, 2).map((p) => p[0]?.toUpperCase() ?? '').join('');

function Metric({ icon, label, value }: { icon: string; label: string; value: string }) {
  return (
    <Card style={{ padding: 14 }}>
      <Icon name={icon} size={20} color="var(--bp-blue)" />
      <div style={{ fontSize: 22, fontWeight: 700, marginTop: 6 }}>{value}</div>
      <div style={{ fontSize: 12, color: 'var(--bp-muted)' }}>{label}</div>
    </Card>
  );
}

export function ProviderDetailPage() {
  const { providerId = '' } = useParams<{ providerId: string }>();
  const api = usePhysicianApi();
  const [provider, setProvider] = useState<ProviderDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [nextStatus, setNextStatus] = useState<ProviderStatus>('ACTIVE');
  const [reason, setReason] = useState('');
  const [saving, setSaving] = useState(false);
  const [savedMsg, setSavedMsg] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    const data = await api.getProvider(providerId);
    setProvider(data);
    if (data) setNextStatus(data.status);
    setLoading(false);
  }, [api, providerId]);

  useEffect(() => {
    void load();
  }, [load]);

  const applyStatus = async () => {
    setSaving(true);
    try {
      await api.setProviderStatus(providerId, { status: nextStatus, reason: reason.trim() || undefined });
      setProvider((p) => (p ? { ...p, status: nextStatus } : p));
      setSavedMsg(`${labels.status[nextStatus]} aplicado`);
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <Card><span style={{ color: 'var(--bp-muted)' }}>{labels.common.loading}</span></Card>;
  if (!provider)
    return (
      <Card style={{ textAlign: 'center', padding: 40 }}>
        <p style={{ color: 'var(--bp-muted)' }}>{labels.providers.empty}</p>
        <Link to="/providers"><BpButton variant="secondary" icon="arrow_back">{labels.common.back}</BpButton></Link>
      </Card>
    );

  const expired = (d?: string) => (d ? new Date(d).getTime() < Date.now() : false);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
      <SectionHeader
        title={provider.displayName}
        subtitle={provider.specialties.join(', ')}
        actions={
          <Link to="/providers">
            <BpButton variant="ghost" icon="arrow_back">
              {labels.common.back}
            </BpButton>
          </Link>
        }
      />

      <div className="grid-2col" style={{ display: 'grid', gridTemplateColumns: '320px 1fr', gap: 18, alignItems: 'start' }}>
        <Card style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <Avatar initials={initials(provider.displayName)} size={52} tone="navy" />
            <div>
              <strong style={{ fontSize: 15 }}>{provider.displayName}</strong>
              <div style={{ marginTop: 4 }}>
                <Pill tone={STATUS_TONE[provider.status]} dot>
                  {labels.status[provider.status]}
                </Pill>
                {provider.stale && (
                  <span style={{ marginLeft: 6 }}>
                    <Pill tone="amber" icon="schedule">
                      Projection stale
                    </Pill>
                  </span>
                )}
              </div>
            </div>
          </div>

          <div>
            <Overline>{labels.providers.status}</Overline>
            <div style={{ marginTop: 8, display: 'flex', flexDirection: 'column', gap: 10 }}>
              <BpSelect value={nextStatus} onChange={(e) => setNextStatus(e.target.value as ProviderStatus)}>
                {STATUSES.map((s) => (
                  <option key={s} value={s}>
                    {labels.status[s]}
                  </option>
                ))}
              </BpSelect>
              <Field label={labels.providers.reason}>
                <BpInput value={reason} onChange={(e) => setReason(e.target.value)} placeholder="Optional" />
              </Field>
              <BpButton icon="save" loading={saving} onClick={() => void applyStatus()}>
                {labels.providers.setStatus}
              </BpButton>
              {savedMsg && (
                <Pill tone="green" icon="check_circle">
                  {savedMsg}
                </Pill>
              )}
            </div>
          </div>
        </Card>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
          <div>
            <Overline>{labels.providers.metrics}</Overline>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))', gap: 12, marginTop: 8 }}>
              <Metric icon="monitoring" label={labels.providers.utilization} value={pct(provider.metrics?.utilization)} />
              <Metric icon="timer" label={labels.providers.duration} value={mins(provider.metrics?.avgDurationSeconds)} />
              <Metric icon="edit_note" label={labels.providers.docTime} value={mins(provider.metrics?.avgDocumentationSeconds)} />
              <Metric icon="person_off" label={labels.providers.noShowRate} value={pct(provider.metrics?.noShowRate)} />
              <Metric icon="wifi_off" label={labels.providers.failureRate} value={pct(provider.metrics?.technicalFailureRate)} />
            </div>
          </div>

          <Card>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
              <Icon name="calendar_month" size={20} color="var(--bp-blue)" />
              <strong>{labels.providers.availability}</strong>
            </div>
            {provider.availability.length === 0 ? (
              <p style={{ color: 'var(--bp-muted)', margin: 0 }}>No availability configured.</p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                {provider.availability.map((w, i) => (
                  <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '8px 10px', background: 'var(--bp-surface-2)', borderRadius: 8 }}>
                    <strong style={{ width: 42 }}>{DAYS[w.dayOfWeek] ?? w.dayOfWeek}</strong>
                    <span style={{ fontFamily: 'ui-monospace, Menlo, monospace' }}>
                      {w.startTime}–{w.endTime}
                    </span>
                    <Pill tone="blue" icon="videocam">
                      {w.mode}
                    </Pill>
                    <span style={{ marginLeft: 'auto', fontSize: 13, color: 'var(--bp-muted)' }}>
                      {labels.providers.maxConcurrent}: {w.maxConcurrent}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </Card>

          <Card>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
              <Icon name="verified_user" size={20} color="var(--bp-blue)" />
              <strong>{labels.providers.credentials}</strong>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              {provider.credentials.map((c, i) => (
                <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '8px 10px', background: 'var(--bp-surface-2)', borderRadius: 8 }}>
                  <div style={{ flex: 1 }}>
                    <strong style={{ fontSize: 14 }}>{c.type}</strong>
                    <div style={{ fontSize: 12, color: 'var(--bp-muted)', fontFamily: 'ui-monospace, Menlo, monospace' }}>
                      {c.number} · vence {c.validUntil}
                    </div>
                  </div>
                  <Pill tone={expired(c.validUntil) ? 'red' : 'green'} icon={expired(c.validUntil) ? 'error' : 'check_circle'}>
                    {expired(c.validUntil) ? labels.providers.credentialExpired : labels.providers.credentialValid}
                  </Pill>
                </div>
              ))}
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
}
