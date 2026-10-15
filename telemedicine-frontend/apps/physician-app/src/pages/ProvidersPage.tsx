import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { usePhysicianApi } from '../api/physicianApi';
import type { ProviderStatus, ProviderSummary } from '../api/types';
import { labels } from '../config/labels';
import { Avatar, BpButton, BpInput, BpSelect, Card, Icon, Pill, SectionHeader, type Tone } from '../ui/primitives';

const STATUSES: ProviderStatus[] = ['ACTIVE', 'SUSPENDED', 'ON_LEAVE', 'OFFBOARDED'];
const STATUS_TONE: Record<ProviderStatus, Tone> = {
  ACTIVE: 'green',
  ON_LEAVE: 'amber',
  SUSPENDED: 'red',
  OFFBOARDED: 'neutral',
};

function initials(name: string) {
  return name.replace(/^Dr[a]?\.\s*/i, '').split(/\s+/).slice(0, 2).map((p) => p[0]?.toUpperCase() ?? '').join('');
}

export function ProvidersPage() {
  const api = usePhysicianApi();
  const [providers, setProviders] = useState<ProviderSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [term, setTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setProviders(await api.searchProviders({ status: statusFilter || undefined }));
    setLoading(false);
  }, [api, statusFilter]);

  useEffect(() => {
    void load();
  }, [load]);

  const visible = providers.filter((p) => {
    const h = `${p.displayName} ${p.specialties.join(' ')} ${p.languages.join(' ')}`.toLowerCase();
    const matchTerm = !term || h.includes(term.toLowerCase());
    const matchStatus = !statusFilter || p.status === statusFilter;
    return matchTerm && matchStatus;
  });

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
      <SectionHeader
        title={labels.providers.title}
        subtitle={`${providers.length} physicians in the directory`}
        actions={
          <BpButton variant="secondary" icon="refresh" onClick={() => void load()}>
            {labels.common.refresh}
          </BpButton>
        }
      />

      <Card style={{ display: 'flex', gap: 12, alignItems: 'center', flexWrap: 'wrap' }}>
        <div style={{ flex: '1 1 260px', position: 'relative', display: 'flex', alignItems: 'center' }}>
          <Icon name="search" size={18} color="var(--bp-muted)" style={{ position: 'absolute', left: 12 }} />
          <BpInput
            placeholder={labels.providers.search}
            value={term}
            onChange={(e) => setTerm(e.target.value)}
            style={{ paddingLeft: 38 }}
          />
        </div>
        <BpSelect value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} style={{ width: 'auto' }}>
          <option value="">All statuses</option>
          {STATUSES.map((s) => (
            <option key={s} value={s}>
              {labels.status[s]}
            </option>
          ))}
        </BpSelect>
      </Card>

      {loading ? (
        <Card>
          <span style={{ color: 'var(--bp-muted)' }}>{labels.common.loading}</span>
        </Card>
      ) : visible.length === 0 ? (
        <Card style={{ textAlign: 'center', padding: 40 }}>
          <Icon name="search_off" size={32} color="var(--bp-muted-2)" />
          <p style={{ color: 'var(--bp-muted)' }}>{labels.providers.empty}</p>
        </Card>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: 14 }}>
          {visible.map((p) => (
            <Card key={p.providerId} style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                <Avatar initials={initials(p.displayName)} size={44} tone="navy" />
                <div style={{ flex: 1, minWidth: 0 }}>
                  <strong style={{ fontSize: 15, display: 'block' }}>{p.displayName}</strong>
                  <span style={{ color: 'var(--bp-muted)', fontSize: 13 }}>{p.specialties.join(', ')}</span>
                </div>
                <Pill tone={STATUS_TONE[p.status]} dot>
                  {labels.status[p.status]}
                </Pill>
              </div>
              <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                {p.modes.map((m) => (
                  <Pill key={m} tone="neutral" icon={m === 'audio' ? 'call' : m === 'chat' ? 'chat' : 'videocam'}>
                    {m}
                  </Pill>
                ))}
                <Pill tone="blue" icon="translate">
                  {p.languages.join(' · ')}
                </Pill>
              </div>
              <Link to={`/providers/${encodeURIComponent(p.providerId)}`} style={{ marginTop: 'auto' }}>
                <BpButton variant="secondary" icon="open_in_new" style={{ width: '100%' }}>
                  {labels.providers.detailTitle}
                </BpButton>
              </Link>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
