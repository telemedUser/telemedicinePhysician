import { useCallback, useEffect, useMemo, useState } from 'react';
import { useApiClient } from '@telemedicine/api-client';
import {
  Button,
  DataTable,
  ErrorMessage,
  FormField,
  LoadingSpinner,
  PageLayout,
  SearchInput,
  Tabs,
  TextField,
} from '@telemedicine/design-system';
import { useTranslation } from '@telemedicine/i18n';

interface AuditEvent {
  auditId: string;
  actorId: string;
  actorRole: string;
  action: string;
  resourceType: string;
  resourceId: string;
  outcome: string;
  occurredAt: string;
  hash?: string;
}

interface AuditQueryResponse {
  events: AuditEvent[];
}

interface ConsentLedgerEntry {
  consentId: string;
  appointmentId: string;
  consentType: string;
  granted: boolean;
  recordedAt: string;
  withdrawnAt?: string;
}

const PAGE_SIZE = 10;

function AuditSearchTab() {
  const { t } = useTranslation('adminAudit');
  const client = useApiClient();
  const [events, setEvents] = useState<AuditEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actor, setActor] = useState('');
  const [action, setAction] = useState('');
  const [resource, setResource] = useState('');
  const [page, setPage] = useState(0);

  const loadAudit = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams();
      if (actor) params.set('actorId', actor);
      if (action) params.set('action', action);
      if (resource) params.set('resourceType', resource);
      params.set('page', String(page));
      params.set('size', String(PAGE_SIZE));
      const query = params.toString();
      const res = await client.get<AuditQueryResponse>(
        `/bff/admin/audit${query ? `?${query}` : ''}`,
      );
      setEvents(res.data?.events ?? []);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load audit log');
    } finally {
      setLoading(false);
    }
  }, [action, actor, client, page, resource]);

  useEffect(() => {
    void loadAudit();
  }, [loadAudit]);

  const filtered = useMemo(() => {
    return events.filter((event) => {
      if (actor && !event.actorId.toLowerCase().includes(actor.toLowerCase())) return false;
      if (action && !event.action.toLowerCase().includes(action.toLowerCase())) return false;
      if (resource && !event.resourceType.toLowerCase().includes(resource.toLowerCase())) return false;
      return true;
    });
  }, [action, actor, events, resource]);

  const pageItems = filtered.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);
  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));

  const exportAudit = async () => {
    try {
      const res = await client.post<{ downloadUrl?: string }>('/v1/audit/exports', {
        format: 'csv',
        actorId: actor || undefined,
        action: action || undefined,
        resourceType: resource || undefined,
      });
      if (res.data?.downloadUrl) {
        window.open(res.data.downloadUrl, '_blank', 'noopener,noreferrer');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Export failed');
    }
  };

  return (
    <>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '1rem', marginBottom: '1rem' }}>
        <FormField label={t('actor')}>
          <SearchInput value={actor} onChange={(e) => setActor(e.target.value)} aria-label={t('actor')} />
        </FormField>
        <FormField label={t('action')}>
          <TextField value={action} onChange={(e) => setAction(e.target.value)} />
        </FormField>
        <FormField label={t('resource')}>
          <TextField value={resource} onChange={(e) => setResource(e.target.value)} />
        </FormField>
        <Button onClick={() => { setPage(0); void loadAudit(); }}>{t('search')}</Button>
        <Button onClick={() => void exportAudit()}>{t('export')}</Button>
      </div>

      {error && <ErrorMessage message={error} />}
      {loading ? (
        <LoadingSpinner label={t('title')} />
      ) : (
        <>
          <DataTable
            columns={[
              { key: 'occurredAt', header: t('timestamp') },
              { key: 'actorId', header: t('actor') },
              { key: 'action', header: t('action') },
              { key: 'resourceType', header: t('resource') },
              { key: 'outcome', header: t('outcome') },
            ]}
            data={pageItems.map((event) => ({
              ...event,
              occurredAt: new Date(event.occurredAt).toLocaleString(),
            }))}
          />
          <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1rem', alignItems: 'center' }}>
            <Button disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
              Previous
            </Button>
            <span>
              {page + 1} / {totalPages}
            </span>
            <Button disabled={page + 1 >= totalPages} onClick={() => setPage((p) => p + 1)}>
              Next
            </Button>
          </div>
        </>
      )}
    </>
  );
}

function ConsentLedgerTab() {
  const { t } = useTranslation('adminAudit');
  const client = useApiClient();
  const [appointmentId, setAppointmentId] = useState('');
  const [entries, setEntries] = useState<ConsentLedgerEntry[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadConsent = async () => {
    if (!appointmentId) return;
    setLoading(true);
    setError(null);
    try {
      const res = await client.get<ConsentLedgerEntry[]>(
        `/v1/audit/consent?appointmentId=${encodeURIComponent(appointmentId)}`,
      );
      setEntries(res.data ?? []);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load consent ledger');
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <div style={{ display: 'flex', gap: '1rem', marginBottom: '1rem', alignItems: 'flex-end' }}>
        <FormField label="Appointment ID">
          <TextField value={appointmentId} onChange={(e) => setAppointmentId(e.target.value)} />
        </FormField>
        <Button disabled={!appointmentId} onClick={() => void loadConsent()}>
          {t('search')}
        </Button>
      </div>
      {error && <ErrorMessage message={error} />}
      {loading ? (
        <LoadingSpinner label={t('consentLedger')} />
      ) : (
        <DataTable
          columns={[
            { key: 'consentType', header: t('resource') },
            { key: 'granted', header: t('outcome') },
            { key: 'recordedAt', header: t('timestamp') },
          ]}
          data={entries.map((entry) => ({
            consentType: entry.consentType,
            granted: entry.granted ? t('hashVerified') : t('hashUnverified'),
            recordedAt: new Date(entry.recordedAt).toLocaleString(),
          }))}
        />
      )}
    </>
  );
}

export default function App() {
  const { t } = useTranslation('adminAudit');
  const [tab, setTab] = useState('audit');

  return (
    <PageLayout>
      <h2>{t('title')}</h2>
      <Tabs
        tabs={[
          { id: 'audit', label: t('title') },
          { id: 'consent', label: t('consentLedger') },
        ]}
        active={tab}
        onChange={setTab}
      />
      {tab === 'audit' ? <AuditSearchTab /> : <ConsentLedgerTab />}
    </PageLayout>
  );
}
