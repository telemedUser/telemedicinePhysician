import { useCallback, useEffect, useMemo, useState } from 'react';
import { useApiClient } from '@telemedicine/api-client';
import { useSession, useJourneyGate } from '@telemedicine/session-context';
import {
  Button,
  PageLayout,
  DataTable,
  EmptyState,
  ErrorMessage,
  LoadingSpinner,
  StatusBadge,
  type BadgeVariant,
} from '@telemedicine/design-system';
import { useTranslation } from '@telemedicine/i18n';

const POLL_INTERVAL_MS = 10000;

interface DocumentSummary {
  documentId: string;
  documentType: string;
  issuedAt: string;
}

interface PrescriptionSummary {
  prescriptionId: string;
  provider: string;
  issuedAt: string;
}

interface MedicalLeaveSummary {
  medicalLeaveId: string;
  durationDays: number;
  issuedAt: string;
}

interface AppointmentDocumentsResponse {
  appointmentId: string;
  downloadUrl?: string | null;
  documents?: DocumentSummary[];
  prescriptions?: PrescriptionSummary[];
  medicalLeaves?: MedicalLeaveSummary[];
}

interface DocumentRow extends Record<string, unknown> {
  id: string;
  type: string;
  issuedAt: string;
  status: string;
  downloadUrl?: string;
}

function docStatusVariant(status: string): BadgeVariant {
  if (status === 'ready') return 'ok';
  if (status === 'pending') return 'pending';
  return 'degraded';
}

function translateDocType(t: (key: string) => string, documentType: string): string {
  const key = documentType.toLowerCase().replace(/[\s-]+/g, '');
  const mapped: Record<string, string> = {
    summary: t('type.summary'),
    consultationsummary: t('type.summary'),
    prescription: t('type.prescription'),
    medicalleave: t('type.medicalLeave'),
  };
  return mapped[key] ?? documentType;
}

function mapResponse(
  data: AppointmentDocumentsResponse,
  t: (key: string) => string,
): DocumentRow[] {
  return [
    ...(data.documents ?? []).map((doc) => ({
      id: doc.documentId,
      type: translateDocType(t, doc.documentType),
      issuedAt: doc.issuedAt,
      status: 'ready',
      downloadUrl: data.downloadUrl ?? undefined,
    })),
    ...(data.prescriptions ?? []).map((rx) => ({
      id: rx.prescriptionId,
      type: translateDocType(t, 'prescription'),
      issuedAt: rx.issuedAt,
      status: 'ready',
      downloadUrl: data.downloadUrl ?? undefined,
    })),
    ...(data.medicalLeaves ?? []).map((leave) => ({
      id: leave.medicalLeaveId,
      type: translateDocType(t, 'medicalLeave'),
      issuedAt: leave.issuedAt,
      status: 'ready',
      downloadUrl: data.downloadUrl ?? undefined,
    })),
  ];
}

export default function App() {
  const { t } = useTranslation('documents');
  const client = useApiClient();
  const { state } = useSession();
  const allowed = useJourneyGate('CONSULTATION_CLOSED');

  const appointmentId = state.appointmentId ?? 'apt-demo';
  const [rows, setRows] = useState<DocumentRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [polling, setPolling] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchDocuments = useCallback(async () => {
    setPolling(true);
    try {
      let data: AppointmentDocumentsResponse | undefined;
      try {
        const res = await client.get<AppointmentDocumentsResponse>(
          `/v1/clinical-docs/${appointmentId}/documents`,
        );
        data = res.data;
      } catch {
        const fallback = await client.get<AppointmentDocumentsResponse>(
          `/v1/clinical-docs/${appointmentId}`,
        );
        data = fallback.data;
      }
      setRows(data ? mapResponse(data, t) : []);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load documents');
    } finally {
      setLoading(false);
      setPolling(false);
    }
  }, [appointmentId, client, t]);

  useEffect(() => {
    if (!allowed) {
      setLoading(false);
      return;
    }
    void fetchDocuments();
    const timer = window.setInterval(() => void fetchDocuments(), POLL_INTERVAL_MS);
    return () => window.clearInterval(timer);
  }, [allowed, fetchDocuments]);

  const tableData = useMemo(
    () =>
      rows.map((row) => ({
        type: row.type,
        issuedAt: row.issuedAt,
        status: t(`status.${row.status as 'ready' | 'pending' | 'error'}`),
        id: row.id,
      })),
    [rows, t],
  );

  const columns = useMemo(
    () => [
      { key: 'type' as const, header: t('title') },
      { key: 'issuedAt' as const, header: t('status.ready') },
      { key: 'status' as const, header: t('status.pending') },
      { key: 'id' as const, header: 'ID' },
    ],
    [t],
  );

  const secureDownload = (url: string) => {
    window.open(url, '_blank', 'noopener,noreferrer');
  };

  if (!allowed) {
    return <EmptyState title={t('blocked.title')} description={t('blocked.description')} />;
  }

  return (
    <PageLayout>
      <h2>{t('title')}</h2>
      {polling && rows.length > 0 && <p>{t('polling')}</p>}
      {loading && rows.length === 0 && <LoadingSpinner />}
      {error && <ErrorMessage message={error} />}
      {!loading && rows.length === 0 && !error && (
        <EmptyState title={t('title')} description={t('empty')} />
      )}
      {rows.length > 0 && (
        <>
          <DataTable columns={columns} data={tableData} />
          <div style={{ marginTop: '1.5rem', display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            {rows.map((row) => (
              <article
                key={row.id}
                style={{
                  border: '1px solid var(--tm-color-border)',
                  borderRadius: 8,
                  padding: 16,
                }}
              >
                <h3 style={{ margin: '0 0 8px' }}>{row.type}</h3>
                <p style={{ margin: '0 0 8px', color: 'var(--tm-color-text-muted)' }}>{row.issuedAt}</p>
                <StatusBadge variant={docStatusVariant(row.status)}>
                  {t(`status.${row.status as 'ready' | 'pending' | 'error'}`)}
                </StatusBadge>
                {row.downloadUrl && (
                  <div style={{ marginTop: 12 }}>
                    <Button variant="secondary" onClick={() => secureDownload(row.downloadUrl!)}>
                      {t('download')}
                    </Button>
                  </div>
                )}
              </article>
            ))}
          </div>
        </>
      )}
    </PageLayout>
  );
}
