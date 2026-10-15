import { useCallback, useEffect, useMemo, useState } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import { useApiClient } from '@telemedicine/api-client';
import {
  Button,
  ErrorMessage,
  FormField,
  Inline,
  LoadingSpinner,
  PageLayout,
  Stack,
  Tabs,
  TextArea,
  TextField,
  ToastContainer,
  type ToastMessage,
} from '@telemedicine/design-system';
import { useTranslation } from '@telemedicine/i18n';
import type {
  MedicationEntry,
  PhysicianDocumentationPayload,
} from '@telemedicine/shared-types';

const AUTOSAVE_MS = 30_000;

type DocTab = 'diagnosis' | 'prescription' | 'medicalLeave';

function draftKey(appointmentId: string) {
  return `physician-doc-draft-${appointmentId}`;
}

function loadDraft(appointmentId: string): PhysicianDocumentationPayload {
  try {
    const raw = sessionStorage.getItem(draftKey(appointmentId));
    return raw ? (JSON.parse(raw) as PhysicianDocumentationPayload) : {};
  } catch {
    return {};
  }
}

function saveDraft(appointmentId: string, draft: PhysicianDocumentationPayload) {
  sessionStorage.setItem(draftKey(appointmentId), JSON.stringify(draft));
}

export default function App() {
  const { t } = useTranslation('physicianDocumentation');
  const { appointmentId: routeId } = useParams<{ appointmentId?: string }>();
  const [searchParams] = useSearchParams();
  const appointmentId = routeId ?? searchParams.get('appointmentId') ?? 'apt-demo';
  const client = useApiClient();

  const [activeTab, setActiveTab] = useState<DocTab>('diagnosis');
  const [draft, setDraft] = useState<PhysicianDocumentationPayload>(() => loadDraft(appointmentId));
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  const tabs = useMemo(
    () => [
      { id: 'diagnosis', label: t('diagnosis') },
      { id: 'prescription', label: t('prescription') },
      { id: 'medicalLeave', label: t('medicalLeave') },
    ],
    [t],
  );

  const pushToast = useCallback((message: string) => {
    const id = crypto.randomUUID();
    setToasts((prev) => [...prev, { id, variant: 'success', message }]);
    window.setTimeout(() => setToasts((prev) => prev.filter((toast) => toast.id !== id)), 3000);
  }, []);

  useEffect(() => {
    setDraft(loadDraft(appointmentId));
  }, [appointmentId]);

  useEffect(() => {
    const timer = window.setInterval(() => {
      saveDraft(appointmentId, draft);
      pushToast(t('savedDraft'));
    }, AUTOSAVE_MS);
    return () => window.clearInterval(timer);
  }, [appointmentId, draft, pushToast, t]);

  const updateDiagnosis = (field: 'icd10' | 'notes', value: string) => {
    setDraft((prev) => ({
      ...prev,
      diagnosis: { ...prev.diagnosis, [field]: value },
    }));
  };

  const updateMedication = (index: number, field: keyof MedicationEntry, value: string) => {
    setDraft((prev) => {
      const medications = [...(prev.prescription?.medications ?? [{ drugName: '', dosage: '', frequency: '' }])];
      medications[index] = { ...medications[index], [field]: value };
      return { ...prev, prescription: { medications } };
    });
  };

  const addMedication = () => {
    setDraft((prev) => ({
      ...prev,
      prescription: {
        medications: [
          ...(prev.prescription?.medications ?? []),
          { drugName: '', dosage: '', frequency: '' },
        ],
      },
    }));
  };

  const updateMedicalLeave = (field: 'startDate' | 'endDate' | 'notes', value: string) => {
    setDraft((prev) => ({
      ...prev,
      medicalLeave: { ...prev.medicalLeave, [field]: value },
    }));
  };

  const submitDocumentation = async () => {
    setSubmitting(true);
    setError(null);
    try {
      saveDraft(appointmentId, draft);
      await client.post(`/bff/physician/documentation/${appointmentId}`, draft);
      sessionStorage.removeItem(draftKey(appointmentId));
      pushToast(t('submit'));
    } catch (err) {
      setError(err instanceof Error ? err.message : t('submit'));
    } finally {
      setSubmitting(false);
    }
  };

  const medications = draft.prescription?.medications ?? [{ drugName: '', dosage: '', frequency: '' }];

  return (
    <PageLayout>
      <Stack>
        <h2>{t('title')}</h2>
        <Tabs tabs={tabs} active={activeTab} onChange={(id) => setActiveTab(id as DocTab)} />

        {activeTab === 'diagnosis' && (
          <Stack>
            <FormField label={t('icd10')} htmlFor="icd10">
              <TextField
                id="icd10"
                value={draft.diagnosis?.icd10 ?? ''}
                onChange={(e) => updateDiagnosis('icd10', e.target.value)}
              />
            </FormField>
            <FormField label={t('notes')} htmlFor="diagnosis-notes">
              <TextArea
                id="diagnosis-notes"
                value={draft.diagnosis?.notes ?? ''}
                onChange={(e) => updateDiagnosis('notes', e.target.value)}
              />
            </FormField>
          </Stack>
        )}

        {activeTab === 'prescription' && (
          <Stack>
            {medications.map((med, index) => (
              <Stack key={index}>
                <FormField label={t('drugName')} htmlFor={`drug-${index}`}>
                  <TextField
                    id={`drug-${index}`}
                    value={med.drugName}
                    onChange={(e) => updateMedication(index, 'drugName', e.target.value)}
                  />
                </FormField>
                <Inline>
                  <FormField label={t('dosage')} htmlFor={`dosage-${index}`}>
                    <TextField
                      id={`dosage-${index}`}
                      value={med.dosage}
                      onChange={(e) => updateMedication(index, 'dosage', e.target.value)}
                    />
                  </FormField>
                  <FormField label={t('frequency')} htmlFor={`frequency-${index}`}>
                    <TextField
                      id={`frequency-${index}`}
                      value={med.frequency}
                      onChange={(e) => updateMedication(index, 'frequency', e.target.value)}
                    />
                  </FormField>
                </Inline>
              </Stack>
            ))}
            <Button variant="secondary" onClick={addMedication}>
              {t('addMedication')}
            </Button>
          </Stack>
        )}

        {activeTab === 'medicalLeave' && (
          <Stack>
            <Inline>
              <FormField label={t('startDate')} htmlFor="leave-start">
                <TextField
                  id="leave-start"
                  type="date"
                  value={draft.medicalLeave?.startDate ?? ''}
                  onChange={(e) => updateMedicalLeave('startDate', e.target.value)}
                />
              </FormField>
              <FormField label={t('endDate')} htmlFor="leave-end">
                <TextField
                  id="leave-end"
                  type="date"
                  value={draft.medicalLeave?.endDate ?? ''}
                  onChange={(e) => updateMedicalLeave('endDate', e.target.value)}
                />
              </FormField>
            </Inline>
            <FormField label={t('notes')} htmlFor="leave-notes">
              <TextArea
                id="leave-notes"
                value={draft.medicalLeave?.notes ?? ''}
                onChange={(e) => updateMedicalLeave('notes', e.target.value)}
              />
            </FormField>
          </Stack>
        )}

        {error && <ErrorMessage message={error} />}
        <Button loading={submitting} onClick={() => void submitDocumentation()}>
          {t('submit')}
        </Button>
        <ToastContainer toasts={toasts} />
      </Stack>
    </PageLayout>
  );
}
