import { useCallback, useEffect, useState } from 'react';
import { useApiClient } from '@telemedicine/api-client';
import {
  Button,
  DataTable,
  ErrorMessage,
  KpiCard,
  LoadingSpinner,
  PageLayout,
  StatusBadge,
  type BadgeVariant,
} from '@telemedicine/design-system';
import { useTranslation } from '@telemedicine/i18n';

interface MonitoringSnapshot {
  activeSessions: number;
  waitingRoomQueue: number;
  errorRate: number;
  connectionQuality?: number | null;
  openAlerts?: number | null;
  sloBurnRate?: number | null;
}

interface LiveSession {
  sessionId: string;
  appointmentId: string;
  provider: string;
  status: string;
  qualityScore?: number;
}

interface MonitoringAlert {
  id: string;
  severity: string;
  message: string;
  status: string;
  runbookUrl?: string;
}

export default function App() {
  const { t } = useTranslation('adminMonitoring');
  const client = useApiClient();
  const [snapshot, setSnapshot] = useState<MonitoringSnapshot | null>(null);
  const [sessions, setSessions] = useState<LiveSession[]>([]);
  const [alerts, setAlerts] = useState<MonitoringAlert[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadData = useCallback(async () => {
    setError(null);
    try {
      const [snapshotRes, sessionsRes, alertsRes] = await Promise.all([
        client.get<MonitoringSnapshot>('/v1/monitoring/snapshot'),
        client.get<LiveSession[]>('/v1/monitoring/sessions').catch(() => ({ data: [] as LiveSession[] })),
        client.get<MonitoringAlert[]>('/v1/monitoring/alerts').catch(() => ({ data: [] as MonitoringAlert[] })),
      ]);
      setSnapshot(snapshotRes.data ?? null);
      setSessions(sessionsRes.data ?? []);
      setAlerts(alertsRes.data ?? []);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load monitoring data');
    } finally {
      setLoading(false);
    }
  }, [client]);

  useEffect(() => {
    void loadData();
    const timer = window.setInterval(() => void loadData(), 60_000);
    return () => window.clearInterval(timer);
  }, [loadData]);

  const acknowledgeAlert = async (alertId: string) => {
    try {
      await client.post(`/v1/monitoring/alerts/${encodeURIComponent(alertId)}/acknowledge`);
      setAlerts((prev) => prev.filter((alert) => alert.id !== alertId));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to acknowledge alert');
    }
  };

  const severityVariant = (severity: string): BadgeVariant => {
    if (severity === 'critical') return 'degraded';
    if (severity === 'warning') return 'pending';
    return 'info';
  };

  const severityLabels: Record<string, string> = {
    critical: t('severity.critical'),
    warning: t('severity.warning'),
    info: t('severity.info'),
  };

  if (loading) {
    return (
      <PageLayout>
        <LoadingSpinner label={t('title')} />
      </PageLayout>
    );
  }

  return (
    <PageLayout>
      <h2>{t('title')}</h2>
      {error && <ErrorMessage message={error} />}

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fill, minmax(160px, 1fr))',
          gap: '1rem',
          marginBottom: '1.5rem',
        }}
      >
        <KpiCard label={t('liveSessions')} value={snapshot?.activeSessions ?? 0} />
        <KpiCard label={t('slo')} value={snapshot?.sloBurnRate?.toFixed(2) ?? '—'} />
        <KpiCard label="Error rate" value={`${((snapshot?.errorRate ?? 0) * 100).toFixed(1)}%`} />
        <KpiCard label={t('alerts')} value={snapshot?.openAlerts ?? alerts.length} />
      </div>

      <section aria-labelledby="sessions-heading">
        <h3 id="sessions-heading">{t('liveSessions')}</h3>
        <DataTable
          columns={[
            { key: 'sessionId', header: 'Session' },
            { key: 'appointmentId', header: 'Appointment' },
            { key: 'provider', header: 'Provider' },
            { key: 'status', header: 'Status' },
            { key: 'qualityScore', header: 'Quality' },
          ]}
          data={sessions.map((session) => ({
            sessionId: session.sessionId,
            appointmentId: session.appointmentId,
            provider: session.provider,
            status: session.status,
            qualityScore: session.qualityScore ?? '—',
          }))}
        />
      </section>

      <section aria-labelledby="alerts-heading" style={{ marginTop: '1.5rem' }}>
        <h3 id="alerts-heading">{t('alerts')}</h3>
        {alerts.length === 0 ? (
          <p style={{ color: 'var(--tm-color-text-muted)' }}>No open alerts</p>
        ) : (
          alerts.map((alert) => (
            <div
              key={alert.id}
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                padding: '12px 0',
                borderBottom: '1px solid var(--tm-color-border)',
                gap: '1rem',
              }}
            >
              <div>
                <StatusBadge variant={severityVariant(alert.severity)}>
                  {severityLabels[alert.severity] ?? alert.severity}
                </StatusBadge>
                <span style={{ marginLeft: 8 }}>{alert.message}</span>
                {alert.runbookUrl && (
                  <a href={alert.runbookUrl} style={{ marginLeft: 8 }} target="_blank" rel="noopener noreferrer">
                    {t('runbook')}
                  </a>
                )}
              </div>
              <Button onClick={() => void acknowledgeAlert(alert.id)}>{t('acknowledge')}</Button>
            </div>
          ))
        )}
      </section>
    </PageLayout>
  );
}
