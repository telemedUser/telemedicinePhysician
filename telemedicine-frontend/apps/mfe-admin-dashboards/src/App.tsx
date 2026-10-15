import { useCallback, useEffect, useMemo, useState } from 'react';
import { Navigate, Route, Routes, useNavigate, useParams } from 'react-router-dom';
import { useApiClient } from '@telemedicine/api-client';
import {
  Button,
  ErrorMessage,
  FormField,
  KpiCard,
  LoadingSpinner,
  MetricBar,
  PageLayout,
  Select,
  Tabs,
} from '@telemedicine/design-system';
import { useTranslation } from '@telemedicine/i18n';

type KpiFamily = 'technical' | 'functional' | 'business';
type Period = 'today' | 'week' | 'month';

interface KpiFamilyResponse {
  family: string;
  metrics: Record<string, number | null>;
}

function periodToDate(period: Period): string {
  const date = new Date();
  if (period === 'week') date.setDate(date.getDate() - 7);
  if (period === 'month') date.setDate(date.getDate() - 30);
  return date.toISOString().slice(0, 10);
}

function KpiDashboardPage({ family }: { family: KpiFamily }) {
  const { t } = useTranslation('adminDashboards');
  const client = useApiClient();
  const [metrics, setMetrics] = useState<Record<string, number | null>>({});
  const [period, setPeriod] = useState<Period>('today');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);

  const loadKpis = useCallback(async () => {
    setRefreshing(true);
    setError(null);
    try {
      const res = await client.get<KpiFamilyResponse>(`/v1/reporting/kpis/${family}`);
      setMetrics(res.data?.metrics ?? {});
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load KPIs');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [client, family]);

  useEffect(() => {
    setLoading(true);
    void loadKpis();
  }, [loadKpis]);

  useEffect(() => {
    const timer = window.setInterval(() => void loadKpis(), 60_000);
    return () => window.clearInterval(timer);
  }, [loadKpis]);

  const exportReport = async (format: 'csv' | 'xlsx') => {
    try {
      const res = await client.post<{ downloadUrl?: string }>('/v1/reporting/exports', {
        format,
        date: periodToDate(period),
      });
      if (res.data?.downloadUrl) {
        window.open(res.data.downloadUrl, '_blank', 'noopener,noreferrer');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Export failed');
    }
  };

  const entries = useMemo(() => Object.entries(metrics), [metrics]);

  if (loading) {
    return <LoadingSpinner label={t('title')} />;
  }

  return (
    <>
      <div style={{ marginBottom: '1rem', display: 'flex', flexWrap: 'wrap', gap: '0.75rem', alignItems: 'flex-end' }}>
        <FormField label={t('period')}>
          <Select value={period} onChange={(e) => setPeriod(e.target.value as Period)}>
            <option value="today">{t('today')}</option>
            <option value="week">{t('week')}</option>
            <option value="month">{t('month')}</option>
          </Select>
        </FormField>
        <Button onClick={() => void loadKpis()} disabled={refreshing}>
          {t('refresh')}
        </Button>
        <Button onClick={() => void exportReport('csv')}>{t('exportCsv')}</Button>
        <Button onClick={() => void exportReport('xlsx')}>{t('exportExcel')}</Button>
      </div>

      {error && <ErrorMessage message={error} />}

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))',
          gap: '1rem',
          marginBottom: '1.5rem',
        }}
      >
        {entries.slice(0, 4).map(([key, value]) => (
          <KpiCard key={key} label={key} value={value ?? '—'} />
        ))}
      </div>

      {entries.map(([key, value]) => (
        <MetricBar
          key={key}
          label={key}
          value={typeof value === 'number' ? value : 0}
          max={100}
        />
      ))}
    </>
  );
}

function DashboardLayout() {
  const { t } = useTranslation('adminDashboards');
  const navigate = useNavigate();
  const { family: familyParam } = useParams();
  const family = (['technical', 'functional', 'business'].includes(familyParam ?? '')
    ? familyParam
    : 'technical') as KpiFamily;

  const tabs = [
    { id: 'technical', label: t('technical') },
    { id: 'functional', label: t('functional') },
    { id: 'business', label: t('business') },
  ];

  return (
    <PageLayout>
      <h2>{t('title')}</h2>
      <Tabs tabs={tabs} active={family} onChange={(id) => navigate(`/admin/dashboards/${id}`)} />
      <KpiDashboardPage family={family} />
    </PageLayout>
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/admin/dashboards/technical" replace />} />
      <Route path="/admin/dashboards" element={<Navigate to="/admin/dashboards/technical" replace />} />
      <Route path="/admin/dashboards/:family" element={<DashboardLayout />} />
    </Routes>
  );
}
